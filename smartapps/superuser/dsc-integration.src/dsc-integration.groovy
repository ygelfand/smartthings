/*
 *  DSC Alarm Panel integration via REST API callbacks
 *
 */

import groovy.json.JsonSlurper;

definition(
    name: "DSC Integration",
    namespace: "",
    author: "Kent Holloway <drizit@gmail.com>",
    description: "DSC Integration App",
    category: "My Apps",
    iconUrl: "https://dl.dropboxusercontent.com/u/2760581/dscpanel_small.png",
    iconX2Url: "https://dl.dropboxusercontent.com/u/2760581/dscpanel_large.png",
    oauth: true
)

import groovy.json.JsonBuilder

preferences {
  section("Alarm Server Settings") {
            //Get IP & Port Address for the AlarmServer
  	input("ip", "text", title: "IP", description: "The IP of your AlarmServer", required: true)
    input("port", "text", title: "Port", description: "The port", required: true)
    //Get Alarm Code
    input("alarmCodePanel", "text", title: "Alarm Code", description: "The code for your alarm panel.", required: true)
    //Allow user to turn off the Smart Monitor Integration if they arn't using it or use it for another purpose
    input "smartMonitorInt", "enum", title: "Integrate w/ Smart Monitor?", options: ["Yes", "No"], required: true
  }
  section("Button for Alarm") {
  	//Grab the DSC Command Switch
    input "thecommand", "capability.Switch", required: false
  }
}

mappings {
  path('/update')            { action: [POST: 'update'] }
  path('/installzones')      { action: [POST: 'installzones'] }
  path('/installpartitions') { action: [POST: 'installpartitions'] }
  path("/panel/:eventcode/:zoneorpart") {
    action: [
      GET: "updateZoneOrPartition"
    ]
  }
}

def installed() {
  log.debug "Installed!"
  initialize()
}

def updated() {
  log.debug "Updated!"
  unsubscribe()
  unschedule()
  initialize()
}
def initialize() {
    //Don't subscribe to the Smart Home Monitor status if user turned it off
    if(smartMonitorInt.value[0] != "N")
    {
        //Subscribe to Smart Home Monitor
        subscribe(location, "alarmSystemStatus", alarmStatusUpdate)
    }
    //Subscribe to button pushes within Device Switch
    subscribe(thecommand, "switch", switchUpdate)
    //Subscribe to responses from sendHubCommand
    subscribe(location, null, lanResponseHandler, [filterEvents:false])
}



def installzones() {
  def children = getChildDevices()
  def zones = request.JSON

  def zoneMap = [
    'contact':'DSC Zone Contact',
    'motion':'DSC Zone Motion',
    'smoke':'DSC Zone Smoke',
    'co':'DSC Zone CO',
    'flood':'DSC Zone Flood',
  ]

  log.debug "children are ${children}"
  for (zone in zones) {
    def id = zone.key
    def type = zone.value.'type'
    def device = zoneMap."${type}"
    def name = zone.value.'name'
    def networkId = "dsczone${id}"
    def zoneDevice = children.find { item -> item.device.deviceNetworkId == networkId }

    if (zoneDevice == null) {
      log.debug "add new child: device: ${device} networkId: ${networkId} name: ${name}"
      zoneDevice = addChildDevice('dsc', "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
    } else {
      log.debug "zone device was ${zoneDevice}"
      try {
        log.debug "trying name update for ${networkId}"
        zoneDevice.name = "${name}"
        log.debug "trying label update for ${networkId}"
        zoneDevice.label = "${name}"
      } catch(IllegalArgumentException e) {
        log.debug "excepted for ${networkId}"
         if ("${e}".contains('identifier required')) {
           log.debug "Attempted update but device didn't exist. Creating ${networkId}"
           zoneDevice = addChildDevice("dsc", "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
         } else {
           log.error "${e}"
         }
      }
    }
  }

  for (child in children) {
    if (child.device.deviceNetworkId.contains('dsczone')) {
      def zone = child.device.deviceNetworkId.minus('dsczone')
      def jsonZone = zones.find { x -> "${x.key}" == "${zone}"}
      if (jsonZone == null) {
        try {
          log.debug "Deleting device ${child.device.deviceNetworkId} ${child.device.name} as it was not in the config"
          deleteChildDevice(child.device.deviceNetworkId)
        } catch(MissingMethodException e) {
          if ("${e}".contains('types: (null) values: [null]')) {
            log.debug "Device ${child.device.deviceNetworkId} was empty, likely deleted already."
          } else {
             log.error e
          }
        }
      }
    }
  }
}

def installpartitions() {
  def children = getChildDevices()
  def partitions = request.JSON

  def partMap = [
    'stay':'DSC Stay Panel',
    'away':'DSC Away Panel',
    'simplestay':'DSC Simple Stay Panel',
    'simpleaway':'DSC Simple Away Panel',
  ]

  log.debug "children are ${children}"
  for (part in partitions) {
    def id = part.key

    for (p in part.value) {
      def type = p.key
      def name = p.value
      def networkId = "dsc${type}${id}"
      def partDevice = children.find { item -> item.device.deviceNetworkId == networkId }
      def device = partMap."${type}"

      if (partDevice == null) {
        log.debug "add new child: device: ${device} networkId: ${networkId} name: ${name}"
        partDevice = addChildDevice('dsc', "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
      } else {
        log.debug "part device was ${partDevice}"
        try {
          log.debug "trying name update for ${networkId}"
          partDevice.name = "${name}"
          log.debug "trying label update for ${networkId}"
          partDevice.label = "${name}"
        } catch(IllegalArgumentException e) {
          log.debug "excepted for ${networkId}"
           if ("${e}".contains('identifier required')) {
             log.debug "Attempted update but device didn't exist. Creating ${networkId}"
             partDevice = addChildDevice('dsc', "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
           } else {
             log.error "${e}"
           }
        }
      }
    }
  }

  for (child in children) {
    for (p in ['stay', 'away']) {
        if (child.device.deviceNetworkId.contains("dsc${p}")) {
        def part = child.device.deviceNetworkId.minus("dsc${p}")
        def jsonPart = partitions.find { x -> x.value."${p}" }
        if (jsonPart== null) {
          try {
            log.debug "Deleting device ${child.device.deviceNetworkId} ${child.device.name} as it was not in the config"
            deleteChildDevice(child.device.deviceNetworkId)
          } catch(MissingMethodException e) {
            if ("${e}".contains('types: (null) values: [null]')) {
              log.debug "Device ${child.device.deviceNetworkId} was empty, likely deleted already."
            } else {
              log.error e
            }
          }
        }
      }
    }
  }
}


void updateZoneOrPartition() {
  update()
}


//Sync changes on your physical alarm panels back to Smart Things.
//Unfortunately, we have to poll for these event changes which can take up to 5 minutes to sync.
//A call back would work, but Alarm Server would need to be overhauled because it doesn't send back the mode (away vs stay)
def lanResponseHandler(evt) {
    def jsonSlurper = new JsonSlurper()
    def systemArmed = false
    def systemEntryDelay = false
    def description = evt.description

    try {
        //Ensure we received at least 4 messages in a CSV format from the sendHubCommand Response
        if (description.count(",") > 4) {
            //Split and decode Base64 the response for the body
            def bodyString = new String(description.split(',')[6].split(":")[1].decodeBase64())
            def resp = jsonSlurper.parseText(bodyString)

            //Make sure I'm seeing a response from my API call and not a call to Arm/Disarm the alarm system
            if(resp.version != null)
            {
                log.debug "Syncing Physical Panel with Smartthings (if needed)"
                //Get Alarm Status (Armed (or Exit Delay) vs Disarmed) - If any partition is armed, the system is "armed"
                def partitions = resp.partition
                partitions.each {k, v ->
                    if(v.status.armed || v.status.exit_delay) {
                        systemArmed = true
                    }
                    if(v.status.entry_delay) {
                        systemEntryDelay = true
                    }
                }

                //Run through the last event messages to determine if armed in Stay or Away mode
                //I dont like this method, but there is no other status that shows this information
                def messages = resp.partition.lastevents
                def found = false

                def filteredMsgs = messages.findAll {it.message.contains("Armed")}
                if (filteredMsgs.size() > 0 ) {
                def lastMsg = filteredMsgs.last().message

                //If the systems entry delay is going off, let's wait to sync.
                if (!systemEntryDelay)
                {
                    //Sync!
                    if (!systemArmed) {
                        log.debug "Physical Panel Disarmed"
                        setCommandSwitch("disarm")
                        setSmartHomeMonitor("off")
                    }
                    else if(lastMsg.contains("Away")) {
                        log.debug "Physical Panel Armed in Away Mode"
                        setCommandSwitch("arm")
                        setSmartHomeMonitor("away")
                    }
                    else if(lastMsg.contains("Stay")) {
                        log.debug "Physical Panel Armed in Stay Mode"
                        setCommandSwitch("stayarm")
                        setSmartHomeMonitor("stay")
                    }
                }
                }
            }
        }

    } catch (e) {
        log.error "something went wrong: $e"
    }
}

private update() {
    def zoneorpartition = params.zoneorpart

    // Add more events here as needed
    // Each event maps to a command in your "DSC Panel" device type
    def eventMap = [
      '601':"zone alarm",
      '602':"zone closed",
      '609':"zone open",
      '610':"zone closed",
      '631':"zone smoke",
      '632':"zone clear",
      '650':"partition ready",
      '651':"partition notready",
      '652':"partition armed",
      '654':"partition alarm",
      '656':"partition exitdelay",
      '657':"partition entrydelay",
      '701':"partition armed",
      '702':"partition armed"
    ]

    // get our passed in eventcode
    def eventCode = params.eventcode
    if (eventCode)
    {
      // Lookup our eventCode in our eventMap
      def opts = eventMap."${eventCode}"?.tokenize()
      // log.debug "Options after lookup: ${opts}"
      // log.debug "Zone or partition: $zoneorpartition"
      if (opts[0])
      {
        // We have some stuff to send to the device now
        // this looks something like panel.zone("open", "1")
        // log.debug "Test: ${opts[0]} and: ${opts[1]} for $zoneorpartition"
        if ("${opts[0]}" == 'zone') {
           //log.debug "It was a zone...  ${opts[1]}"
           updateZoneDevices(zonedevices,"$zoneorpartition","${opts[1]}")
        }
        if ("${opts[0]}" == 'partition') {
           //log.debug "It was a zone...  ${opts[1]}"
           updatePartitions(paneldevices, "$zoneorpartition","${opts[1]}")
        }
      }
    }
}

private updateZoneDevices(zonedevices,zonenum,zonestatus) {
  log.debug "zonedevices: $zonedevices - ${zonenum} is ${zonestatus}"
  // log.debug "zonedevices.id are $zonedevices.id"
  // log.debug "zonedevices.displayName are $zonedevices.displayName"
  // log.debug "zonedevices.deviceNetworkId are $zonedevices.deviceNetworkId"
  def zonedevice = zonedevices.find { it.deviceNetworkId == "zone${zonenum}" }
  if (zonedevice) {
      log.debug "Was True... Zone Device: $zonedevice.displayName at $zonedevice.deviceNetworkId is ${zonestatus}"
      //Was True... Zone Device: Front Door Sensor at zone1 is closed
      zonedevice.zone("${zonestatus}")
    }
}

private updatePartitions(paneldevices, partitionnum, partitionstatus) {
  log.debug "paneldevices: $paneldevices - ${partitionnum} is ${partitionstatus}"
  def paneldevice = paneldevices.find { it.deviceNetworkId == "partition${partitionnum}" }
  if (paneldevice) {
    log.debug "Was True... Panel device: $paneldevice.displayName at $paneldevice.deviceNetworkId is ${partitionstatus}"
    //Was True... Zone Device: Front Door Sensor at zone1 is closed
    paneldevice.partition("${partitionstatus}", "${partitionnum}")
  }
}

private sendMessage(msg) {
    def newMsg = "Alarm Notification: $msg"
    if (phone1) {
        sendSms(phone1, newMsg)
    }
    if (sendPush == "Yes") {
        sendPush(newMsg)
    }
}

def switchUpdate(evt) {
    def eventMap = [
        'stayarm':"/api/alarm/stayarm",
        'disarm':"/api/alarm/disarm",
        'arm':"/api/alarm/armwithcode"
    ]

    def securityMonitorMap = [
        'stayarm':"stay",
        'disarm':"off",
        'arm':"away"
    ]

    def path = eventMap."${evt.value}"
    setSmartHomeMonitor(securityMonitorMap."${evt.value}")
    callAlarmServer(path)
}

//When a button is pressed in Smart Home Monitor, this will capture the event and send that to Alarm Server
//It will also sync the status change over to the DSC Command Switch
def alarmStatusUpdate(evt) {
    def eventMap = [
        'stay':"/api/alarm/stayarm",
        'off':"/api/alarm/disarm",
        'away':"/api/alarm/armwithcode"
    ]

    def securityMonitorMap = [
        'stay':"stayarm",
        'off':"disarm",
        'away':"arm"
    ]

    def command = securityMonitorMap."${evt.value}";
    setCommandSwitch(command)
    def path = eventMap."${evt.value}"
    callAlarmServer(path)
}

private callAlarmServer(path) {
    try {
        sendHubCommand(new physicalgraph.device.HubAction(
            method: "GET",
            path: path,
            headers: [
                HOST: "${ip}:${port}"
            ],
            query: [alarmcode: "${alarmCodePanel.value}"]
        ))
    } catch (e) {
        log.error "something went wrong: $e"
    }
}

private setCommandSwitch(command)
{
    //Let's make sure the switch isn't already set to that value
    if(thecommand.currentSwitch != command)
    {
        log.debug "Set Command Switch to $command"
        thecommand."$command"()
    }
}

private setSmartHomeMonitor(status)
{
    //Let's make sure the user turned on Smart Home Monitor Integration and the value I'm trying to set it to isn't already set
    if(smartMonitorInt.value[0] != "N" && location.currentState("alarmSystemStatus").value != status)
    {
        log.debug "Set Smart Home Monitor to $status"
        sendLocationEvent(name: "alarmSystemStatus", value: status)
    }
}
