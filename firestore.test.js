const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date(Date.now() - 1000);
}

function validProfilePayload(userId) {
  return {
    userId,
    displayName: "Alex",
    goal: "build_muscle",
    experienceLevel: "day_one",
    equipmentAccess: "full_gym",
    sportFocus: "General Gym",
    dietaryStyle: "balanced",
    daysPerWeek: 4,
    dailyCalorieTarget: 2250,
    dailyProteinTargetG: 145,
    dailyCarbsTargetG: 260,
    dailyFatTargetG: 70,
    onboardingCompleted: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
}

function validWorkoutDayPayload(userId) {
  return {
    userId,
    dayNumber: 1,
    title: "Upper Body Confidence",
    focusArea: "Chest, Shoulders & Back",
    estimatedMinutes: 45,
    isRestDay: false,
    exercises: [
      {
        id: "ex1",
        name: "Machine Chest Press",
        muscleGroup: "Chest",
        sets: 3,
        reps: "10-12",
        restSeconds: 75,
        suggestedWeightKg: 20,
        setupCue: "Adjust seat so handles align with mid-chest.",
        formMistakeToAvoid: "Do not shrug shoulders toward your ears.",
        alternativeExercise: "Dumbbell Floor Press",
        alternativeCue: "Keep elbows at 45 degrees and press smoothly.",
      },
    ],
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
}

function validMealPayload(userId) {
  return {
    userId,
    dateKey: "2026-11-15",
    mealSlot: "lunch",
    name: "Paneer Rice Bowl",
    calories: 560,
    proteinG: 34,
    carbsG: 62,
    fatG: 18,
    isAiSuggested: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
}

function validWorkoutLogPayload(userId) {
  return {
    userId,
    dateKey: "2026-11-15",
    dayTitle: "Upper Body Confidence",
    durationMinutes: 42,
    completedExercises: 5,
    totalSetsCompleted: 15,
    feeling: "great",
    notes: "Felt smooth on chest press!",
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
}

function validBuddyMessagePayload(userId) {
  return {
    userId,
    role: "user",
    text: "How low should I squat if my ankles feel tight?",
    contextTag: "Goblet Squat",
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
}

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

test("Unauthenticated user cannot read or write profile or subcollections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("mealLogs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("workoutDays").get());
});

test("Owner can create, read, and update their profile; shadow field fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(profileRef.set(validProfilePayload(ALICE_UID)));
  await assertSucceeds(profileRef.get());
  await assertSucceeds(
    profileRef.update({
      dailyCalorieTarget: 2450,
      updatedAt: nowTs(),
    })
  );

  // Shadow field injection must fail
  await assertFails(
    profileRef.update({
      isAdmin: true,
      updatedAt: nowTs(),
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's profile, workouts, meals, or chats", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workoutDays").doc("day_1").set(validWorkoutDayPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("mealLogs").doc("meal_1").set(validMealPayload(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID)));
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("workoutDays").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("mealLogs").doc("meal_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("mealLogs").doc("meal_2").set(validMealPayload(BOB_UID)));
});

test("Owner can create and list workoutDays, workoutLogs, mealLogs, and buddyChats", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(userRef.collection("workoutDays").doc("day_1").set(validWorkoutDayPayload(ALICE_UID)));
  await assertSucceeds(userRef.collection("workoutDays").get());

  await assertSucceeds(userRef.collection("workoutLogs").doc("log_1").set(validWorkoutLogPayload(ALICE_UID)));
  await assertSucceeds(userRef.collection("workoutLogs").get());

  await assertSucceeds(userRef.collection("mealLogs").doc("meal_1").set(validMealPayload(ALICE_UID)));
  await assertSucceeds(userRef.collection("mealLogs").get());

  await assertSucceeds(userRef.collection("buddyChats").doc("msg_1").set(validBuddyMessagePayload(ALICE_UID)));
  await assertSucceeds(userRef.collection("buddyChats").get());
});

test("Invalid enum or bounds fail validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const badMeal = validMealPayload(ALICE_UID);
  badMeal.mealSlot = "midnight_feast";
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("mealLogs").doc("bad_meal").set(badMeal)
  );

  const badProfile = validProfilePayload(ALICE_UID);
  badProfile.daysPerWeek = 99;
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(badProfile));
});
