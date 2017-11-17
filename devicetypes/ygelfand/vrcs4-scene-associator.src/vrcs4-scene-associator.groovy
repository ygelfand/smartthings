/*
 *  VRCS4 Scene Associator
 *
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
 metadata {
	// Automatically generated. Make future change here.
	definition (name: "VRCS4 Scene Associator", namespace: "ygelfand", author: "ygelfand") {
		capability "Actuator"
        capability "Button"
        capability "Configuration"
        capability "Sensor"
        
        attribute "associatedLoad", "STRING"
        attribute "associatedLoadId", "STRING"
        attribute "currentButton", "STRING"
        attribute "numButtons", "STRING"
        command "associateLoad", ["NUMBER"]
        command "setButton", ["NUMBER", "STRING"]
 				command "getparamState"       
 				command "setLightStatus"       
        
		fingerprint deviceId: "0x0100", inClusters:"0x85, 0x2D, 0x7C, 0x77, 0x82, 0x73, 0x86, 0x72, 0x91, 0xEF, 0x2B, 0x2C"
	}

   	/*preferences {
            input "button1", "string", title: "Button 1 Network id", description: "button 1 network id", required: false
            input "button2", "string", title: "Button 2 Network id", description: "button 2 network id", required: false
            input "button3", "string", title: "Button 3 Network id", description: "button 3 network id", required: false
            input "button4", "string", title: "Button 4 Network id", description: "button 4 network id", required: false
     }*/
	simulator {
		status "button 1 pushed":  "command: 2B01, payload: 01 FF"
		status "button 2 pushed":  "command: 2B01, payload: 02 FF"
		status "button 3 pushed":  "command: 2B01, payload: 03 FF"
		status "button 4 pushed":  "command: 2B01, payload: 04 FF"
    status "button released":  "command: 2C02, payload: 00"
	}

	tiles {
		standardTile("button", "device.button", width: 2, height: 2) {
			state "default", label: " ", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffffff"

		}
        
        // Configure button.  Syncronize the device capabilities that the UI provides
		standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}
      standardTile("getmyState", "device.switch", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
        	state "default", label:"Get My State", action:"getparamState"
		}

		main "button"
		details (["button", "configure", "getmyState"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
    
    def result = null
	def cmd = zwave.parse(description)
	if (cmd) {
		result = zwaveEvent(cmd)
	}
	return result

}
def setButton(num, val) {
	updateState("button$num", val)

}
// Handle a button being pressed
def buttonEvent(button) {
	button = button as Integer
	def result = []
    
    
    updateState("currentButton", "$button")
        
    if (button > 0) {    
        // update the device state, recording the button press
        result << createEvent(name: "button", value: /*"pushed"*/ "button $button", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was pushed", isStateChange: true)
	}
    else {    
        // update the device state, recording the button press
        result << createEvent(name: "button", value: "default", descriptionText: "$device.displayName button was released", isStateChange: true)
        
    }
    
    result
}
def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
    createEvent(name: "button", value: "default", descriptionText: "$device.displayName bzone button was pressed", isStateChange: true)
}
// A zwave command for a button press was received
def zwaveEVent (physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfReport cmd) {
    log.debug "bleh $cmd"
    
}
def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd) {

	// The controller likes to repeat the command... ignore repeats
	if (state.lastScene == cmd.sceneId && (state.repeatCount < 4) && (now() - state.repeatStart < 2000)) {
    	log.debug "Button ${cmd.sceneId} repeat ${state.repeatCount}x ${now()}"
        state.repeatCount = state.repeatCount + 1
        createEvent([:])
    }
    else {
    	// If the button was really pressed, store the new scene and handle the button press
        state.lastScene = cmd.sceneId
        state.lastLevel = 0
        state.repeatCount = 0
        state.repeatStart = now()

        buttonEvent(cmd.sceneId)
    }
}

// A scene command was received -- it's probably scene 0, so treat it like a button release
def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfGet cmd) {
	log.debug "confget; $cmd"
	buttonEvent(cmd.sceneId)
}

// The controller sent a scene activation report.  Log it, but it really shouldn't happen.
def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfReport cmd) {
    log.debug "Scene activation report"
	log.debug "Scene ${cmd.sceneId} set to ${cmd.level}"
    
    createEvent([:])
}


// Configuration Reports are replys to configuration value requests... If we knew what configuration parameters
// to request this could be very helpful.
def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	createEvent([:])
}

// The VRC supports hail commands, but I haven't seen them.
def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd) {
    createEvent([name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false])
}

// Update manufacturer information when it is reported
def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	if (state.manufacturer != cmd.manufacturerName) {
		updateDataValue("manufacturer", cmd.manufacturerName)
	}
    
    createEvent([:])
}

// Association Groupings Reports tell us how many groupings the device supports.  This equates to the number of
// buttons/scenes in the VRCS
def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
	def response = []
    
    log.debug "${getDataByName("numButtons")} buttons stored"
	if (getDataByName("numButtons") != "$cmd.supportedGroupings") {
		updateState("numButtons", "$cmd.supportedGroupings")
        log.debug "${cmd.supportedGroupings} groups available"
        response << createEvent(name: "numButtons", value: cmd.supportedGroupings, displayed: false)
        
        response << associateHub()
	}    
    else { 	
    	response << createEvent(name: "numButtons", value: cmd.supportedGroupings, displayed: false)
    }
    return response
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
	def commands = []
    def buttonlist = ['x', state.button1.split(','), state.button2.split(','), state.button3.split(','), state.button4.split(',')]
    log.debug buttonlist
    cmd.nodeId.each({log.debug "AssociationReport: '${cmd}', hub: '$zwaveHubNodeId' reports nodeId: '$it' is associated in group: '${cmd.groupingIdentifier}'"
    	if((it != zwaveHubNodeId) && (!buttonlist[integer(cmd.groupingIdentifier)].contains(Integer.toHexString(it).toUpperCase())))
        {
        	 log.debug "$it couldnt find " + Integer.toHexString(it).toUpperCase() + "in button ${cmd.groupingIdentifier}"
        	 sendHubCommand(new physicalgraph.device.HubAction(zwave.associationV2.associationRemove(groupingIdentifier: cmd.groupingIdentifier, nodeId: it).format()))
        }
    })
    [:]
}
def zwaveEvent(physicalgraph.zwave.commands.associationv1.AssociationReport cmd) {
	def commands = []
    cmd.nodeId.each({log.debug "AssociationReport v1: '${cmd}', hub: '$zwaveHubNodeId' reports nodeId: '$it' is associated in group: '${cmd.groupingIdentifier}'"
        //commands << zwave.associationV1.associationRemove(groupingIdentifier: cmd.groupingIdentifier, nodeId: it).format()
    })
    log.debug "sending $commands"
  //  return delayBetween(commands,100)
    //[:]
}

// Handles all Z-Wave commands we don't know we are interested in
def zwaveEvent(physicalgraph.zwave.Command cmd) {	
    createEvent([:])
}

// handle commands

// Create a list of the configuration commands to send to the device
def configurationCmds() {
	// Always check the manufacturer and the number of groupings allowed
	def commands = [
    	zwave.manufacturerSpecificV2.manufacturerSpecificGet().format(),
		zwave.associationV1.associationGroupingsGet().format()
    ]
    
    commands = commands + associateHub()

    // Reset to sceneId 0 (no scene) initially to turn off all LEDs.
    //commands << zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: 255, level: 255, sceneId: 0).format()
    //commands << zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: 0, level: 255, sceneId: 1).format()
   // commands << zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: 0, level: 255, sceneId: 2).format()
  //  commands << zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: 0, level: 255, sceneId: 3).format()

    delayBetween(commands)
}

def getparamState() {
		log.debug "Listing of current parameter settings of ${device.displayName}"
    def cmds = []
    sendHubCommand(new physicalgraph.device.HubAction("91001D0D01FF01180508D3"))
    log.debug "sending $cmds --- ${device.deviceNetworkId}"
}
def setLightStatus(one,two,three,four)
{
    def hidden = ["0F", "00", "13", device.deviceNetworkId ]
    def start = ["91","00", "1D", "0D", "01", "FF"]
    def end = [ "00", "00", "0A"]
		log.debug "$one - $two - $three - $four"
    def light = integer(one) + (integer(two) << 1) + (integer(three) << 2) + (integer(four) << 3)
    def checksum = 255
    hidden.each { checksum^=(integerhex(it)) }
    start.each { checksum^=(integerhex(it)) }
    checksum^=light
    log.debug "LIGHT: $light"
    end.each { checksum^=(integerhex(it)) }
    log.debug "${start.join()}${String.format("%02X",light)}${end.join()}${Integer.toHexString(checksum).toUpperCase()}"
    sendHubCommand(new physicalgraph.device.HubAction("${start.join()}${String.format("%02X",light)}${end.join()}${String.format("%02X",checksum)}"))
}
// Configure the device
def configure() {
		def cmd=configurationCmds()
    log.debug("Sending configuration: ${cmd}")
    return cmd
}

// Associate a load with the button, or clear the association if nodeid = 0
//
// nodeId:  a hex string, ie 4E for the Z-Wave node number
def associateLoad(String nodeId) {
	def node = integerhex(nodeId)
    
	if (node != 0) {
    	updateState("associatedLoad", "1")
        updateState("associatedLoadId", nodeId)
		log.debug "Node $nodeId associated with button 1"
    }
    else {
    	updateState("associatedLoad", "0")
        log.debug "No nodes associated with button 1"
    }
   
   	configure()
}

// Associate the hub with the buttons on the device, so we will get status updates
def associateHub() {
    def commands = []
   // commands << zwave.remoteAssociationActivateV1.remoteAssociationActivate(groupingIdentifier:2).format()
    for (def buttonNum = 1; buttonNum <= integer(getDataByName("numButtons")); buttonNum++) {
       	commands << zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:buttonNum, sceneId:0).format()
    	//commands << zwave.remoteAssociationActivateV1.remoteAssociationActivate(groupingIdentifier:buttonNum).format()
        
        		def buttonlist = ['x', state.button1, state.button2, state.button3, state.button4]
                if(buttonlist[buttonNum]) {
             //   commands << zwave.associationV1.associationRemove(groupingIdentifier: buttonNum, nodeId: zwaveHubNodeId).format()
                commands << zwave.associationV2.associationSet(groupingIdentifier: buttonNum, nodeId: [zwaveHubNodeId] + integerhex(buttonlist[buttonNum]) ).format()
              //  commands << zwave.associationV2.associationSet(groupingIdentifier: buttonNum, nodeId:  integerhex(buttonlist[buttonNum]) ).format()
       	}
        
        
        //commands << zwave.sceneControllerConfV1.sceneControllerConfGet(groupId:buttonNum ).format()
    	commands << zwave.associationV2.associationGet(groupingIdentifier:buttonNum).format()
    	//commands << zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId:buttonNum, level: 32, dimmingDuration: 255, override: false).format()
    	commands << zwave.sceneActuatorConfV1.sceneActuatorConfGet(sceneId:buttonNum).format()
    	//commands << zwave.sceneActuatorConfV1.sceneActuatorConfSet(sceneId:buttonNum, level: 255, override: false).format()
    	//commands << zwave.associationV2.associationReport(groupingIdentifier:buttonNum).format()
    	//commands << zwave.associationV2.associationSpecificGroupReport (group:buttonNum).format()

	}    
    
    return commands
}

// Update State
// Store mode and settings
def updateState(String name, String value) {
	state[name] = value
	device.updateDataValue(name, value)
}

// Get Data By Name
// Given the name of a setting/attribute, lookup the setting's value
def getDataByName(String name) {
	state[name] ?: device.getDataValue(name)
}

//Stupid conversions

// convert a double to an integer
def integer(double v) {
	return v.toInteger()
}

// convert a hex string to integer
def integerhex(String v) {
	if (v == null) {
    	return 0
    }
    
	return v.split(',').collect { Integer.parseInt(it, 16) }
}

def integer(String v) {
	if (v == null) {
    	return 0
    }
    
	return Integer.parseInt(v)
}
