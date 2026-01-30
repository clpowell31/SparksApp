// FIX: Explicitly import from "v1" to use Gen 1 syntax
import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";

admin.initializeApp();

// Trigger when a new message is added to ANY chat
export const sendChatNotification = functions.firestore
  .document("chats/{chatId}/messages/{messageId}")
  .onCreate(async (snap: any, context: any) => {
    const message = snap.data();
    const chatId = context.params.chatId;
    const senderId = message.senderId;

    // 1. Get Chat Metadata
    const chatRef = admin.firestore().collection("conversations").doc(chatId);
    const chatDoc = await chatRef.get();
    const chatData = chatDoc.data();

    if (!chatData) {
      console.log("No chat data found");
      return null;
    }

    const userIds = chatData.users as string[];
    const recipientId = userIds.find((uid) => uid !== senderId);

    if (!recipientId) {
      console.log("No recipient found");
      return null;
    }

    // 2. Fetch Sender's Name
    const senderDoc = await admin.firestore().collection("users")
      .doc(senderId).get();
    const senderName = senderDoc.data()?.firstName || "Someone";

    // 3. Fetch Recipient's FCM Token
    const recipientDoc = await admin.firestore().collection("users")
      .doc(recipientId).get();
    const recipientData = recipientDoc.data();

    if (!recipientData || !recipientData.fcmToken) {
      console.log("Recipient has no FCM token");
      return null;
    }

    const fcmToken = recipientData.fcmToken;
    const encryptedKey = message.encryptionKeys?.[recipientId] || "";

    // 4. Construct the Data Payload
    const payload = {
      data: {
        chatId: chatId,
        senderId: senderId,
        senderName: senderName,
        encryptedContent: message.text,
        encryptedKey: encryptedKey,
      },
      token: fcmToken,
    };

    // 5. Send via FCM
    try {
      await admin.messaging().send(payload);
      console.log(`Notification sent to ${recipientId}`);
    } catch (error) {
      console.error("Error sending notification:", error);
    }

    return null;
  });