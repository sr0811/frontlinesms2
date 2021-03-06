package frontlinesms2

import org.apache.camel.Exchange
import org.apache.camel.Processor

class MessageStorageService implements Processor {
	public void process(Exchange ex) {
		def log = { println "MessageStorageService.process() : $it" }
		log "headers # $ex.in.headers"
		def message = ex.in.body
		assert message instanceof Fmessage
		message = message.id ? Fmessage.findById(message.id) : message
		def conn = Fconnection.findById(ex.in.headers."${Fconnection.HEADER_FCONNECTION_ID}")
		message.save(flush:true)
		conn.addToMessages(message)
		conn.save(flush:true)
	}
}
