package frontlinesms2

import grails.converters.JSON


class ContactController extends ControllerUtils {
//> STATIC PROPERTIES
	static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

//> SERVICES
	def grailsApplication
	def contactSearchService

//> INTERCEPTORS
	def beforeInterceptor = {
		params.max = params.max?: grailsApplication.config.grails.views.pagination.max
		params.sort = params.sort ?: 'name'
		params.offset = params.offset ?: 0
		true
	}

//> ACTIONS
	def index() {
		redirect action: "show", params:params
	}

	def getUniqueCustomFields() {
		def allFields = CustomField.getAllUniquelyNamed()
		withFormat {
			json {
				render([uniqueCustomFields:allFields] as JSON)
			}
		}
	}
	
	def updateContactPane() {
		def contactInstance = Contact.get(params.id)
		def usedFields = contactInstance?.customFields ?: []
		def usedFieldNames = []
		usedFields.each() { field ->
			usedFieldNames.add(field.name)
		}
		def allFields = CustomField.getAllUniquelyNamed()
		def unusedFields = []
		allFields.each() {
			if(!usedFieldNames.contains(it))
				unusedFields.add(it)
		}
		def model = [contactInstance: contactInstance,
				contactGroupInstanceList: contactInstance?.groups ?: [],
				contactFieldInstanceList: usedFields,
				contactGroupInstanceTotal: contactInstance?.groups?.size() ?: 0,
				nonContactGroupInstanceList: contactInstance ? Group.findAllWithoutMember(contactInstance) : null,
				uniqueFieldInstanceList: unusedFields,
				fieldInstanceList: CustomField.findAll(),
				groupInstanceList: Group.findAll(),
				smartGroupInstanceList: SmartGroup.list()]
		render view:'/contact/_single_contact_view', model:model
	}
	
	def show() {
		if(params.flashMessage) {
			flash.message = params.flashMessage
		}
		def contactList = contactSearchService.contactList(params)
		def contactInstanceList = contactList.contactInstanceList
		def contactInstanceTotal = Contact.count()
		def contactInstance = (params.contactId ? Contact.get(params.contactId) : (contactInstanceList ? contactInstanceList[0] : null))
		def usedFields = contactInstance?.customFields ?: []
		def usedFieldNames = []
		usedFields.each() { field ->
			usedFieldNames.add(field.name)
		}
		def allFields = CustomField.getAllUniquelyNamed()
		def unusedFields = []
		allFields.each() {
			if(!usedFieldNames.contains(it))
				unusedFields.add(it)
		}
		def contactGroupInstanceList = contactInstance?.groups ?: []
		if(params.contactId && !contactInstance) {
			flash.message = message(code:'contact.not.found')
			redirect(action: 'show')
			return false
		} else if(params.groupId && !contactList.contactsSection) {
			flash.message = message(code:'group.not.found')
			redirect(action: 'show')
			return false
		} else if(params.smartGroupId && !contactList.contactsSection) {
			flash.message = message(code:'smartgroup.not.found')
			redirect(action: 'show')
			return false
		}

		[contactInstance: contactInstance,
				checkedContactList: ',',
				contactInstanceList: contactInstanceList,
				contactInstanceTotal: contactInstanceTotal,
				contactsSection: contactList.contactsSection,
				contactsSectionContactTotal: contactList.contactsSectionContactTotal,
				contactFieldInstanceList: usedFields,
				contactGroupInstanceList: contactGroupInstanceList,
				contactGroupInstanceTotal: contactGroupInstanceList.size(),
				nonContactGroupInstanceList: contactInstance ? Group.findAllWithoutMember(contactInstance) : null,
				uniqueFieldInstanceList: unusedFields,
				fieldInstanceList: CustomField.findAll(),
				groupInstanceList: Group.findAll(),
				smartGroupInstanceList: SmartGroup.list()]
	}
	
	def createContact() {
		render view:'show', model: [contactInstance: new Contact(params),
				contactFieldInstanceList: [],
				contactGroupInstanceList: [],
				contactGroupInstanceTotal: 0,
				nonContactGroupInstanceList: Group.findAll(),
				uniqueFieldInstanceList: CustomField.getAllUniquelyNamed(),
				fieldInstanceList: CustomField.findAll(),
				groupInstanceList: Group.findAll(),
				smartGroupInstanceList: SmartGroup.list()] << contactSearchService.contactList(params)
	}

	def saveContact() {
		def contactInstance = Contact.get(params.contactId) ?: new Contact()
		contactInstance.properties = params
		if(attemptSave(contactInstance)) {
			parseContactFields(contactInstance)
			attemptSave(contactInstance)
		}
		redirect(action:'show', params:[contactId:contactInstance.id])
	}
	
	def update() {
		withContact { contactInstance ->
			contactInstance.properties = params
			parseContactFields(contactInstance)
			attemptSave(contactInstance)
			if(params.groupId) redirect(controller: params.contactsSection, action: 'show', id: params.groupId, params:[contactId: contactInstance.id, sort:params.sort, offset: params.offset])
			else redirect(action:'show', params:[contactId: contactInstance.id, offset:params.offset], max:params.max)
		}
	}
	
	def updateMultipleContacts() {
		getCheckedContacts().each { c ->
			parseContactFields(c)
			attemptSave(c)
		}
		render(view:'show', model: show())
	}
	
	def confirmDelete() {
		def contactInstanceList = getCheckedContacts()
		[contactInstanceList:contactInstanceList,
				contactInstanceTotal:contactInstanceList.size()]
	}
	
	def delete() {
		getCheckedContacts()*.delete()
		flash.message = message(code:'default.deleted', args:[message(code:'contact.label')])
		redirect(action: "show")		
	}

	def newCustomField() {
		def contactInstance = params.contactId
		def customFieldInstance = new CustomField()
		customFieldInstance.properties = params
		[customFieldInstance: customFieldInstance,
				contactInstance: contactInstance]
	}

	def search() {
		fsms.render template:'search_results', model:contactSearchService.contactList(params)
	}
	
	def checkForDuplicates() {
		def foundContact = Contact.findByMobile(params.mobile)
		if (foundContact && foundContact.id.toString() == params.contactId) {
			render true
		} else
			if(!foundContact && params.mobile) render true
			else render false
	}
	
	def messageStats() {
		def contactInstance = Contact.get(params.id)
		if(contactInstance) {
			def messageStats = [inboundMessagesCount: contactInstance.inboundMessagesCount, outboundMessagesCount: contactInstance.outboundMessagesCount]
			render messageStats as JSON
		}
	}

//> PRIVATE HELPER METHODS
	private def attemptSave(contactInstance) {
		def mobile = params.mobile?.replaceAll(/\D/, '')
		if(params.mobile && params.mobile[0] == '+') mobile = '+' + mobile
		def existingContact = mobile ? Contact.findByMobileLike(mobile) : null
		if (existingContact && existingContact != contactInstance) {
			flash.message = "${message(code: 'contact.exists.warn')}  " + g.link(action:'show', params:[contactId:Contact.findByMobileLike(params.mobile)?.id], g.message(code: 'contact.view.duplicate'))
			return false
		}
		if (contactInstance.save()) {
			flash.message = message(code:'default.updated', args:[message(code:'contact.label'), contactInstance.name])
			def redirectParams = [contactId: contactInstance.id]
			if(params.groupId) redirectParams << [groupId: params.groupId]
			return true
		}
		return false
	}

	def multipleContactGroupList() {
		def groups = Group.getGroupLists(getCheckedContactIds())
		render(view: "_multiple_contact_view", model: [sharedGroupInstanceList:groups.shared,
				nonSharedGroupInstanceList:groups.nonShared])
	}
	
	private def withContact = withDomainObject Contact, { params.contactId }

	private def getCheckedContacts() {
		Contact.getAll(getCheckedContactIds()) - null
	}

	private def getCheckedContactIds() {
		def ids = params['contact-select']?:
				params.checkedContactList? params.checkedContactList.tokenize(',').unique():
				[params.contactId]
		return ids.flatten().unique()
	}

	private def parseContactFields(Contact contactInstance) {
		updateCustomFields(contactInstance)
		updateGroups(contactInstance)
		return contactInstance.stripNumberFields()
	}
	
	private def updateGroups(Contact contactInstance) {
		def groupsToAdd = params.groupsToAdd.tokenize(',').unique()
		def groupsToRemove = params.groupsToRemove.tokenize(',')
		
		// Check for errors in groupsToAdd and groupsToRemove
		if(!groupsToAdd.disjoint(groupsToRemove)) {
			contactInstance.errors.reject(message(code: 'contact.addtogroup.error'))
			return false
		}
		
		groupsToRemove.each() { id ->
			contactInstance.removeFromGroups(Group.get(id))
		}
		groupsToAdd.each() { id ->
			contactInstance.addToGroups(Group.get(id))
		}
		return contactInstance
	}
	
	private def updateCustomFields(Contact contactInstance) {
		def fieldsToAdd = params.fieldsToAdd ? params.fieldsToAdd.tokenize(',') : []
		def fieldsToRemove = params.fieldsToRemove ? params.fieldsToRemove.tokenize(',') : []
		
		fieldsToAdd.each() { name ->
			def existingFields = CustomField.findAllByNameAndContact(name, contactInstance)
			def fieldsByName = params."$name"
			if(fieldsByName?.class != String) {
				fieldsByName.each() { val ->
					if(val != "" && !existingFields.value.contains(val))
						contactInstance.addToCustomFields(new CustomField(name: name, value: val)).save(flush:true)
						existingFields = CustomField.findAllByNameAndContact(name, contactInstance)
				}
			} else if(fieldsByName != "" && !existingFields.value.contains(fieldsByName)) {
				contactInstance.addToCustomFields(new CustomField(name: name, value: fieldsByName))
			}
		}
		fieldsToRemove.each() { id ->
			def toRemove = CustomField.get(id)
			contactInstance.removeFromCustomFields(toRemove)
			if(toRemove)
				toRemove.delete()
		}

		//also save any existing fields that have changed
		def existingFields = CustomField.findAllByContact(contactInstance)
		existingFields.each() { existingField ->
			def newValue = params."$existingField.name"
			if (newValue && (existingField.value != newValue))
			{
				existingField.value = newValue
				existingField.save()
			}
		}

		return contactInstance
	}
}
