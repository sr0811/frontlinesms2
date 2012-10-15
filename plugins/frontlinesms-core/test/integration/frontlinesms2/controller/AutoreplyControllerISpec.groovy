package frontlinesms2.controller

import frontlinesms2.*

import spock.lang.*

class AutoreplyControllerISpec extends grails.plugin.spock.IntegrationSpec {
	def controller
	
	def setup() {
		controller = new AutoreplyController()
	}
	
	@Unroll
	def 'can create an Autoreply'() {
		given:
			controller.params.name = name
			controller.params.keywords = keyword
			controller.params.messageText = autoreplyText
		when:
			controller.save()
		then:
			def autoreply = Autoreply.findByName(name)
			autoreply.autoreplyText == autoreplyText
			autoreply.keywords?.size() == 1
			autoreply.keywords[0].value == keyword
		where:
			name     | keyword | autoreplyText
			"Color"  | 'COLOR' | "ahhhhhhhhh"
			"Thanks" | ''      | "Thank you for the text"
	}
	
	@Unroll
	def "can edit an Autoreply"() {
		given: 'an autoreply exists'
			def k = new Keyword(value:initialKeyword)
			def a = new Autoreply(name:"Color", autoreplyText:"ahhhhhhhhh")
					.save(flush:true, failOnError:true)
			a.addToKeywords(k)
		and: 'controller params are setup'
			controller.params.ownerId = a.id
			controller.params.name = name
			controller.params.keywords = finalKeyword
			controller.params.messageText = autoreplyText
			controller.response.format = 'html'
			
		when:
			def model = controller.save()
			
		then: 'the auto reply has been updated'
			def autoreply = Autoreply.get(model.ownerId)
			autoreply.name == name
			autoreply.keywords[0].value == finalKeyword
			autoreply.autoreplyText == autoreplyText
		
		and: 'the old auto reply and keyword have been deleted'
			Keyword.findByValue(initialKeyword) == null
			Autoreply.findByName("Color") == null
			
		where:
			name      | initialKeyword | finalKeyword | autoreplyText
			"ColorZ"  | "COLOR"        | "COLORZ"     | "blue, i mean green"
			"Blank"   | "COLOR"        | ""           | "blue, i mean green"
			"ColorZ"  | ""             | "COLORZ"     | "blue, i mean green"
	}  

	def "can create Autoreply with multiple keywords"(){
		given:
			controller.params.name = 'Fruit'
			controller.params.keywords = 'Mango,Orange,Banana'
			controller.params.messageText = 'Some Text'
			controller.response.format = 'html'
		when:
			def model = controller.save()
		then:
			def autoreply = Autoreply.get(model.ownerId)
			autoreply.autoreplyText == 'Some Text'
			autoreply.keywords?.size() == 3
			autoreply.keywords[0].value == 'MANGO'
			autoreply.keywords[1].value == 'ORANGE'
			autoreply.keywords[2].value == 'BANANA'
	}

	def "can change Autoreply keywords"(){
		given: 'an autoreply exists'
			def a = new Autoreply(name:"Fruits", autoreplyText:"Hello")
			a.addToKeywords(new Keyword(value:"MANGO"))
			a.addToKeywords(new Keyword(value:"ORANGE"))
			a.save(flush:true, failOnError:true)
		and: 'controller params are setup'
			controller.params.ownerId = a.id
			controller.params.name = "Matunda"
			controller.params.keywords = "Apple,Strawberry"
			controller.params.messageText = "Hello"
			controller.response.format = 'html'
		when:
			def model = controller.save()
			println "MODEL::: $model"
			def autoreply = Autoreply.get(model?.ownerId)
		then: 'the auto reply has been updated'
			autoreply != null
			autoreply.name == "Matunda"
			autoreply.keywords[0].value == "APPLE"
			autoreply.keywords[1].value == "STRAWBERRY"
			autoreply.autoreplyText == "Hello"
		and: 'the old keyword have been deleted'
			Keyword.findByValue("ORANGE") == null
	}

	def "can edit Autoreply with multiple keywords, keeping some of the old ones"(){
		given: 'an autoreply exists'
			def a = new Autoreply(name:"Fruits", autoreplyText:"Hello")
			a.addToKeywords(new Keyword(value:"MANGO"))
			a.addToKeywords(new Keyword(value:"ORANGE"))
			a.save(flush:true, failOnError:true)
		and: 'controller params are setup'
			controller.params.ownerId = a.id
			controller.params.name = "Matunda"
			controller.params.keywords = "Mango,Banana,Ovacado"
			controller.params.messageText = "Hello"
			controller.response.format = 'html'
		when:
			def model = controller.save()
			println "MODEL::: $model"
			def autoreply = Autoreply.get(model?.ownerId)
		then: 'the auto reply has been updated'
			autoreply != null
			autoreply.name == "Matunda"
			autoreply.keywords[0].value == "MANGO"
			autoreply.keywords[1].value == "BANANA"
			autoreply.keywords[2].value == "OVACADO"
			autoreply.autoreplyText == "Hello"
		and: 'the old keyword have been deleted'
			Keyword.findByValue("ORANGE") == null
	}
}

