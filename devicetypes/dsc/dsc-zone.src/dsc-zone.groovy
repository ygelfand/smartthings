/*
 *  DSC Zone Device
 *
 *  Author: Matt Martz <matt.martz@gmail.com>
 *  Date: 2014-04-28
 */

// for the UI
metadata {
  definition (name: "DSC Zone", author: "me@me.com", namespace: "dsc" ) {
    // Change or define capabilities here as needed
    capability "Refresh"
    capability "Contact Sensor"
    capability "Motion Sensor"
    capability "Sensor"
    capability "Momentary"
    capability "Polling"

    // Add commands as needed
    command "zone"
  }

  simulator {
    // Nothing here, you could put some testing stuff here if you like
  }

  tiles {
    // Main Row
    standardTile("zone", "device.contact", width: 2, height: 2, canChangeBackground: true, canChangeIcon: true) {
      state "open",   label: '${name}', icon: "st.contact.contact.open",   backgroundColor: "#ffa81e"
      state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
      state "alarm",  label: '${name}', icon: "st.contact.contact.open",   backgroundColor: "#ff0000"
      state "bypass",  label: '${name}', icon: "st.contact.contact.open",   backgroundColor: "#c88ad1"
    }
    valueTile("lastEvent", "state.lastEvent", inactiveLabel: false, width: 2, height: 2) {
            state "lastEvent", label:'${currentValue}%', unit:""
        }
    // This tile will be the tile that is displayed on the Hub page.
    main "zone"

    // These tiles will be displayed when clicked on the device, in the order listed here.
    details(["zone"])
  }
}

// handle commands
def zone(String status) {
  // state will be a valid state for a zone (open, closed)
  // zone will be a number for the zone
  //state.lastEvent = now()
 // updateDataValue("lastEvent", now());
 // log.debug "Zone: ${status}"
  sendEvent (name: "contact", value: "${status}")
}

def poll() {
  log.debug "Executing 'poll'"
  // TODO: handle 'poll' command
  // On poll what should we do? nothing for now..
}

def refresh() {
  log.debug "Executing 'refresh' which is actually poll()"
  poll()
  // TODO: handle 'refresh' command
}