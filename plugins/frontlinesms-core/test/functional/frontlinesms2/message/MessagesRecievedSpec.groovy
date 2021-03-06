package frontlinesms2.message

import frontlinesms2.*
import frontlinesms2.utils.*

class MessagesRecievedSpec extends MessageBaseSpec {
	
	def 'new messages in the current section are checked for in the backround every ten seconds and causes a notification to appear if there are new messages'() {
		when:
			createInboxTestMessages()
			to PageMessageInbox
		then:
			messageList.messages.size() == 2
		when:
			sleep 11000
		then:
			messageList.messages.size() == 2
			!messageList.newMessageNotification.displayed
		when:
			createTestMessages()
			sleep 5000
		then:
			messageList.messages.size() == 2
			!messageList.newMessageNotification.displayed
		when:
			sleep 5000
		then:
			waitFor { messageList.newMessageNotification.displayed }
	}
	
	def 'clicking the new message notification refreshes the list and removes the notification'() {
		when:
			createInboxTestMessages()
			to PageMessageInbox
		then:
			messageList.messages.size() == 2
		when:
			createTestMessages()
			sleep 11000
		then:
			waitFor { messageList.newMessageNotification.displayed }
			messageList.messages.size() == 3
		when:
			messageList.newMessageNotification.find("a").click()
		then:
			waitFor { messageList.messages.size() == 5 }
			!messageList.newMessageNotification.displayed
	}

	def 'when clicking the new message notification, the view stays at the current page and details'() {
		when:
			createInboxTestMessages()
			to PageMessageInbox, Fmessage.findBySrc('Bob').id
		then:
			messageList.messages.size() == 2
		when:
			createTestMessages()
			sleep 11000
		then:
			waitFor { messageList.newMessageNotification.displayed }
		when:
			messageList.newMessageNotification.find("a").click()
		then:
			waitFor { singleMessageDetails.displayed }
			waitFor { singleMessageDetails.sender == "Bob" }
	}
}
