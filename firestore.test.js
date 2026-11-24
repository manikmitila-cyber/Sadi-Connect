const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const HOST_UID = "host_alice_123";
const CTRL_UID = "ctrl_bob_456";
const INTRUDER_UID = "eve_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function nowMinus(ms = 1000) {
  return new Date(Date.now() - ms);
}

test("Unauthenticated users cannot access profiles, devices, directory, or sessions", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(HOST_UID).get());
  await assertFails(unauth.collection("device_directory").doc("469752988").get());
  await assertFails(unauth.collection("sessions").doc("sess_1").get());
});

test("User profile and device registration enforce ownership and schema parity", async () => {
  const hostDb = testEnv.authenticatedContext(HOST_UID).firestore();
  const intruderDb = testEnv.authenticatedContext(INTRUDER_UID).firestore();
  const ts = nowMinus(5000);

  await assertSucceeds(
    hostDb.collection("users").doc(HOST_UID).set({
      userId: HOST_UID,
      displayName: "Abdur Rahman",
      email: "abdur@example.com",
      preferredLanguage: "bn",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user read/write must fail (PII isolation)
  await assertFails(intruderDb.collection("users").doc(HOST_UID).get());
  await assertFails(
    intruderDb.collection("users").doc(HOST_UID).update({
      displayName: "Hijacked",
      updatedAt: nowMinus(500),
    })
  );

  // Register device under /users/{uid}/devices/{deviceId}
  await assertSucceeds(
    hostDb.collection("users").doc(HOST_UID).collection("devices").doc("dev_1").set({
      userId: HOST_UID,
      deviceName: "Galaxy A55",
      deviceModel: "SM-A556E",
      deviceCode: "469752988",
      online: true,
      favorite: false,
      localIp: "192.168.1.25",
      p2pPort: 45450,
      accessibilityReady: true,
      screenCaptureReady: true,
      cameraReady: true,
      lastSeenAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // List devices for owner succeeds; intruder fails
  await assertSucceeds(hostDb.collection("users").doc(HOST_UID).collection("devices").get());
  await assertFails(intruderDb.collection("users").doc(HOST_UID).collection("devices").get());

  // Device directory lookup by exact 9-digit code succeeds, unfiltered list fails for non-owner
  await assertSucceeds(
    hostDb.collection("device_directory").doc("469752988").set({
      ownerUid: HOST_UID,
      deviceId: "dev_1",
      deviceCode: "469752988",
      deviceName: "Galaxy A55",
      ownerDisplayName: "Abdur Rahman",
      online: true,
      localIp: "192.168.1.25",
      p2pPort: 45450,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const ctrlDb = testEnv.authenticatedContext(CTRL_UID).firestore();
  await assertSucceeds(ctrlDb.collection("device_directory").doc("469752988").get());
  await assertFails(ctrlDb.collection("device_directory").get());
  await assertSucceeds(
    hostDb.collection("device_directory").where("ownerUid", "==", HOST_UID).get()
  );
});

test("Session approval security: controller cannot pre-approve or bypass owner approval", async () => {
  const hostDb = testEnv.authenticatedContext(HOST_UID).firestore();
  const ctrlDb = testEnv.authenticatedContext(CTRL_UID).firestore();
  const intruderDb = testEnv.authenticatedContext(INTRUDER_UID).firestore();
  const ts = nowMinus(5000);

  const baseSession = {
    hostUid: HOST_UID,
    hostDeviceId: "dev_1",
    hostDeviceName: "Galaxy A55",
    hostName: "Abdur Rahman",
    hostDeviceCode: "469752988",
    hostLocalIp: "192.168.1.25",
    hostP2pPort: 45450,
    controllerUid: CTRL_UID,
    controllerName: "Sadiq",
    controllerDeviceName: "Redmi Note 13",
    memberUids: [HOST_UID, CTRL_UID],
    joinToken: "tok_998877",
    status: "pending",
    screenShareAllowed: false,
    remoteTouchAllowed: false,
    remoteCameraAllowed: false,
    voiceCallState: "idle",
    voiceCallerUid: "",
    cameraFacing: "back",
    videoQuality: "medium",
    targetFps: 20,
    brightnessPercent: 70,
    expiresAtMillis: Date.now() + 3600000,
    createdAt: ts,
    updatedAt: ts,
  };

  // Controller trying to create a pre-approved session MUST FAIL
  await assertFails(
    ctrlDb.collection("sessions").doc("sess_evil").set({
      ...baseSession,
      status: "approved",
      screenShareAllowed: true,
      remoteTouchAllowed: true,
    })
  );

  // Controller creating a pending session request SUCCEEDS
  await assertSucceeds(ctrlDb.collection("sessions").doc("sess_1").set(baseSession));

  // Remote commands MUST FAIL while session is still pending
  await assertFails(
    ctrlDb.collection("sessions").doc("sess_1").collection("commands").doc("cmd_1").set({
      memberUids: [HOST_UID, CTRL_UID],
      senderUid: CTRL_UID,
      type: "tap",
      x: 0.5,
      y: 0.5,
      endX: 0.5,
      endY: 0.5,
      value: 0,
      executed: false,
      createdAt: nowMinus(500),
    })
  );

  // Controller CANNOT self-approve
  await assertFails(
    ctrlDb.collection("sessions").doc("sess_1").update({
      status: "approved",
      screenShareAllowed: true,
      remoteTouchAllowed: true,
      updatedAt: nowMinus(500),
    })
  );

  // Host approves session and grants screenShare + remoteTouch
  await assertSucceeds(
    hostDb.collection("sessions").doc("sess_1").update({
      status: "approved",
      screenShareAllowed: true,
      remoteTouchAllowed: true,
      updatedAt: nowMinus(400),
    })
  );

  // Now controller can send a valid remote tap command
  await assertSucceeds(
    ctrlDb.collection("sessions").doc("sess_1").collection("commands").doc("cmd_1").set({
      memberUids: [HOST_UID, CTRL_UID],
      senderUid: CTRL_UID,
      type: "tap",
      x: 0.5,
      y: 0.5,
      endX: 0.5,
      endY: 0.5,
      value: 0,
      executed: false,
      createdAt: nowMinus(300),
    })
  );

  // Chat messages scoped to members only
  await assertSucceeds(
    ctrlDb.collection("sessions").doc("sess_1").collection("messages").doc("msg_1").set({
      memberUids: [HOST_UID, CTRL_UID],
      senderUid: CTRL_UID,
      senderName: "Sadiq",
      text: "Assalamu Alaikum",
      delivered: false,
      createdAt: nowMinus(200),
    })
  );

  await assertSucceeds(
    hostDb
      .collection("sessions")
      .doc("sess_1")
      .collection("messages")
      .where("memberUids", "array-contains", HOST_UID)
      .get()
  );

  // Intruder cannot read messages or sessions
  await assertFails(
    intruderDb.collection("sessions").doc("sess_1").collection("messages").doc("msg_1").get()
  );
  await assertFails(intruderDb.collection("sessions").where("memberUids", "array-contains", CTRL_UID).get());

  // Terminal state lock: once ended, cannot be reactivated
  await assertSucceeds(
    hostDb.collection("sessions").doc("sess_1").update({
      status: "ended",
      updatedAt: nowMinus(100),
    })
  );
  await assertFails(
    hostDb.collection("sessions").doc("sess_1").update({
      status: "approved",
      updatedAt: nowMinus(50),
    })
  );
});
