package frontlinesms2.domain

import frontlinesms2.*

class RadioShowISpec extends grails.plugin.spock.IntegrationSpec {

	def setup() {
		def radioShow = new RadioShow(name: "Health & fitness")
		radioShow.addToMessages(new Fmessage(text: "eat fruits", starred : true, date: new Date(), inbound: true, src: 'src'))
		radioShow.addToMessages(new Fmessage(text: "eat vegetables", date: new Date(), inbound: true, src: 'src'))
		radioShow.addToMessages(new Fmessage(text: "excerise", date: new Date(), inbound: true, src: 'src'))
		radioShow.save(failOnError: true, flush: true)
	}

	def "should fetch all messages for a radio show"() {
		when:
			def results = RadioShow.findByName("Health & fitness").getShowMessages(false).list()
		then:
			results*.text.containsAll(["eat fruits", "eat vegetables", "excerise"])
	}

	def "should fetch all messages based on offset and max parameters"() {
		when:
			def results = RadioShow.findByName("Health & fitness").getShowMessages(false).list(max:2, offset: 0)
		then:
			results.size() == 2
	}

	def "should fetch all starred radio show messages"() {
		when:
			def results = RadioShow.findByName("Health & fitness").getShowMessages(['starred':true]).list()
		then:
			results*.text == ["eat fruits"]
	}

	def "should count starred radio show messages"() {
		when:
			def starredHealthShowMessages = RadioShow.findByName("Health & fitness").getShowMessages(true)
			def unstarredHealthShowMessages = RadioShow.findByName("Health & fitness").getShowMessages(false)
		then:
			starredHealthShowMessages.count() == 1
			unstarredHealthShowMessages.count() == 3
	}
}
