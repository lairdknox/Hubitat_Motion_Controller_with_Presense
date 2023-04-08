/************************************************************************
Copyright 2023 Jeffrey Knox

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
import groovy.transform.Field
************************************************************************/
import groovy.transform.Field

/************************************************************************
IN-MEMORY ONLY VARIABLES (Cleared only on HUB REBOOT or CODE UPDATES)
************************************************************************/
@Field volatile static Map<String, Map> sensorActive = 		[ : ]

definition(
	name: "Motion Zone Presense (Child App)",
	namespace:	"jlkSpace",
	author:		"Jeff Knox",
	parent: "jlkSpace:Motion Zone Presense",
	description: "Motion zone controller with cooldown.",
	category:	"Convenience",
	iconUrl:	"",
	iconX2Url:	""
)

preferences {
   page name: "mainPage"
}

def mainPage() {
	dynamicPage(title: "Motion Zone with Presence (child)", name: "mainPage", install: true, uninstall: true) {
		section("Configure motion zone") {
			paragraph "Creates a motion zone that stays active longer the more times it is triggered."
			label description: "Enter a name for this zone:", required: true
			input name: "sensors", type: "capability.motionSensor",	title: "Select motion sensors for this zone",	required: true, multiple: true
			input "contact", "capability.contactSensor", title: "Door Contact Sensor", required: false, multiple: false
			input "debugLogging", "bool", title: "Enable debug logging"
		}
	}
}

void installed() {
   initialize()
}

void updated() {
   initialize()
	subscribe(sensors, "motion", "motionHandler")
}

void initialize() {
	log.debug "initialize() child app - ${app.name} - ${app.id} - ${app.label}"
	unsubscribe()
	unschedule()
	def motionDev = getChildDevice("PresenceZone_${app.id}")
	if(!motionDev) {
		motionDev = addChildDevice("jlkSpace", "Virtual Motion with Cooldown", "PresenceZone_${app.id}", null, [name: "zone ${app.label}"])
		} else {
		motionDev.name = "zone ${app.label}"
	}
}

def motionHandler(evt) {
	def motionDev = getChildDevice("PresenceZone_${app.id}")
	def zoneActive = false
	
	if (evt.value != "inactive") {
		for (sensor in sensors) {
			if (sensorActive.("${sensor.id}")) {
				zoneActive = true
			}
		}
		sensorActive.("${evt.deviceId}") = true
		if (!zoneActive) {
			motionDev.active()
		}
	} else {
		sensorActive.("${evt.deviceId}") = false
		for (sensor in sensors) {
			if (sensorActive.("${sensor.id}")) {
				zoneActive = true
			}
		}
		if (!zoneActive) {
			log.debug "turn off sensor"
			motionDev.inactive()
		}	
	}
}

void ll(msg) {
    if (debugLogging) log.debug(msg)
}
