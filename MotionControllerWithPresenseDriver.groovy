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

metadata {
	definition (name: "Virtual Motion with Cooldown", namespace: "jlkSpace", author: "Jeff Knox") {
		capability "Sensor"
		capability "Actuator"
		capability "Motion Sensor"
		capability "Contact Sensor"
		capability "LocationMode"
		
		attribute "counter", "number"
		attribute "delay", "string"
		attribute "cooldown", "string"
		
		attribute "cooldown low", "string"
		attribute "cooldown med", "string"
		attribute "cooldown high", "string"

		command "active"
		command "inactive"
		command "open"
		command "closed"
		command "configure"
	}

	preferences {
		input "baseDelay", "number", title: "Enter base delay duration", defaultValue: 60, submitOnChange: true,
			description: "<font size='1em'>Delay controls how long the cooldown period is before the lights turn off after motion stops</font>"
		input name: "sensitivity", type: "enum", title: "Sensitivity", defaultValue: "High", options: ["Low","Medium", "High"],
			description: "<font size='1em'>Sensitivity determins the length of the cooldown timer. Low sensitivity has a shorter delay while High produces a longer delay.</font>"
		input "reportCooldown", "bool", title: "Enable cooldown state", defaultValue: true,
			description: "<font size='1em'>When enabled device will use three motions states (active, cooldown, inactive).</font>"
		input name: "specialModes", type: "text", title: "Override Delay Modes",
			description: "<font size='1em'>Enter a comma delimited list of modes to use the override delay value instead of the base delay value.</font>"
		input "overrideDelay", "number", title: "Enter override delay duration", defaultValue: 0, submitOnChange: true,
			description: "<font size='1em'>Modes listed in the Override field use this value for the cooldown delay instead of the default value.</font>"
		input "debugLogging", "bool", title: "Enable debug logging", defaultValue: true
	}
}

def installed() {
	ll "installed(${device})"
	updated()
}

/************************************************************************
 Initialze variables

 Get list of orived modes and split list into array for easy 
 manipulation. Trim white space from mode names.
************************************************************************/

def updated() {
	ll "updated(${device})"
	if (state.data == null) state.data = [counter: 0, delay: 0, interval: 0, delayStr: "---", intervalStr: "---", contact: "open"]
	updateInterval()
	sendEvent(name: "counter", value: state.data.counter)
	sendEvent(name: "cooldown", value: state.data.intervalStr)
	sendEvent(name: "delay", value: state.data.delayStr)
	sendEvent(name: "contact", value: state.data.contact)
	if (debugLogging) runIn(10800, turnOffDebugging)
	def overrideModes = ["*No modes selected*"]
	if (specialModes != null) {
		overrideModes = []
		overrideModes = specialModes.split(",")
		for (i in 0..<overrideModes.size()) {
			overrideModes[i] = overrideModes[i].trim()
		}
	}
	state.data.overrides = overrideModes
	//state.remove("override")
}

def configure() {
	ll "configure(${device})"
	updated()
}

/************************************************************************
 Handle "on" and "active" events. Set basic delay based on if override 
 exists for this zone. Limit counter to max value of 12.

 Handle contact sensor: Do not increment counter if door closed. No need 
 to run up counter since lights will not turn off while door closed.
************************************************************************/

def on() {
	if (state.data == null) updated()
	if (state.data.contact == "open") state.data.counter += 1
	if (state.data.counter > 12) state.data.counter = 12
	ll "on(${device}): Counter: ${state.data.counter}"
	if (state.data.overrides.contains(location.mode)) {
		state.data.delay = overrideDelay
	} else {
		state.data.delay = baseDelay
	}
	updateInterval()
	unschedule(cooldownHandler)
	sendEvent(name: "motion", value: "active")
	sendEvent(name: "counter", value: state.data.counter)
	sendEvent(name: "cooldown", value: state.data.intervalStr)
	sendEvent(name: "delay", value: state.data.delayStr)
}

/************************************************************************
 Handle "off" and "inactive" events. Use the cooldown parameter to 
 determine when to decrement the counter.

 First run:		Call cooldown in calculated seconds
 Remaining run: Call cooldown in base delay seconds
************************************************************************/

def off() {
	if (state.data == null) updated()
	ll "off(${device}): Counter: ${state.data.counter}, Cooldown: ${state.data.intervalStr}"
	if (reportCooldown) {
		sendEvent(name: "motion", value: "cooldown")
	} else {
		sendEvent(name: "motion", value: "active")
	}
	runIn (state.data.interval, "cooldownHandler")
	state.data.interval = baseDelay // <-- Is this needed?
}

def active(){
	log.info "${device} is active"
	if (state.data == null) updated()
	on()
}

def inactive(){
	log.info "${device} is inactive"
	if (state.data == null) updated()
	off()
}

/************************************************************************
 Decrement the counter unless the door contact switch is closed. If 
 counter reaches "0" then set motion to inactive.

 If enable cooldown state is true (default) then the virtual sensor will 
 use three motion states (active, cooldown, inactive). Setting it to 
 false will only send two states (active and inactve) and may be required 
 for some applications.
************************************************************************/

def cooldownHandler() {
	ll "cooldownHandler(${device}): Counter: ${state.data.counter}, Cooldown: ${state.data.delayStr}"
	if (state.data.contact == "open") state.data.counter -= 1
	if (state.data.counter < 0) state.data.counter = 0

	if (reportCooldown) {
		sendEvent(name: "motion", value: "cooldown")
	} else {
		sendEvent(name: "motion", value: "active")
	}
	sendEvent(name: "counter", value: state.data.counter)
	sendEvent(name: "delay", value: state.data.delayStr)
	sendEvent(name: "cooldown", value: state.data.delayStr)
	if (state.data.counter < 1) {

	sendEvent(name: "motion", value: "inactive")
		sendEvent(name: "cooldown", value: "0:00")
	} else {
		if (state.data.overrides.contains(location.mode)) {
			state.data.counter = 0
			sendEvent(name: "counter", value: state.data.counter)
			sendEvent(name: "motion", value: "inactive")
			sendEvent(name: "cooldown", value: "0:00")
		} else {
			runIn (state.data.delay, "cooldownHandler")
		}
	}
}

/************************************************************************
 Calculate the interval value based on the sensitivity setting and 
 counter value. Create string representations of the values.
************************************************************************/

def updateInterval() {
	ll "updateInterval(${device})"
	def counter = state.data.counter
	if (counter == 0) counter = 1
	switch (sensitivity) {
		case "Low":
		state.data.interval = (1 + ((counter - 1) / 10)) * state.data.delay as Integer
		break

		case "Medium":
		state.data.interval = (Math.log(counter ** 1.5) + 1) * state.data.delay as Integer
		break

		default:
		state.data.interval = (Math.log(counter ** 3) + 1) * state.data.delay as Integer
		break
	}
	ll "Interval: ${state.data.interval}"
	def itvl = "0${state.data.interval%60}"
	state.data.intervalStr = "${Math.floor(state.data.interval/60).toInteger()}:${itvl.substring(itvl.length()-2)}"
	def dly = "0${state.data.delay%60}"
	state.data.delayStr = "${Math.floor(state.data.delay/60).toInteger()}:${dly.substring(dly.length()-2)}"
	ll "--> ${state.data.intervalStr} - ${state.data.delayStr}"
}

def open() {
	if (state.data.size() == 0 || state.data == null) updated()
	log.info "${device} contact switch open"
	state.data.contact = "open"
	sendEvent(name: "contact", value: state.data.contact)
	off()
}

def closed() {
	if (state.data.size() == 0 || state.data == null) updated()
	log.info "${device} contact switch closed"
	state.data.contact = "closed"
	state.data.counter += 1
	sendEvent(name: "contact", value: state.data.contact)
	on()
}

def turnOffDebugging(){
    device.updateSetting("debugLogging", [value:false, type:"bool"])
}

def ll(msg) {
	if (settings?.debugLogging || settings?.debugLogging == null) log.debug "$msg"
}
