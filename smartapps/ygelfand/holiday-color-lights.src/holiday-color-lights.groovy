/**
 *  Holiday Color Lights
 *
 *  Copyright 2016 ygelfand
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
definition(
    name: "Holiday Color Lights",
    namespace: "ygelfand",
    author: "ygelfand",
    description: "This SmartApp will change the color of selected lights based on closest holiday colors",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "configurationPage")
    }
def configurationPage() {
dynamicPage(name: "configurationPage", title: "Holidays setup",uninstall: true, install: true) {
		section("HolidaySchedule") {
            input name: "sunset",type: "bool", title: "Start at sunset?", required: true, defaultValue: true, submitOnChange: true
        if(sunset == false) {
            input "starttime", "time", title: "Start Time", required: true
        }
            input "sunrise","bool", title: "End at sunrise?", required: true, defaultValue: true, submitOnChange: true
        if(sunrise == false) {
			input "endtime", "time", title: "End Time", required: true
        }
        	input "days", "enum", title: "Day of the week (none for all)", required: false, multiple: true, options: ["Sunday":"Sunday", "Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday", "Saturday":"Saturday"]
		}
 
	section("Light Settings") {
            input "lights", "capability.colorControl", title: "Which Color Changing Bulbs?", multiple:true, required: true
        	input "brightnessLevel", "number", title: "Brightness Level (1-100)?", required:false, defaultValue:100
	}
	section("Holidays Settings") {
		input "holidays", "enum", title: "Holidays?", required: true, multiple: true, options: holidayNames(), defaultValue: holidayNames()
        input "maxdays", "number", title: "Maximum number of days around a holiday? (-1 for unlimited)", required: true, defaultValue: -1
        input "forceholiday", "enum", title: "Force a specific holiday?", required: false, multiple: false, options: holidayNames()
	}
section("Frequency") {
            input "cycletime", "enum", title: "Cycle frequency?" , options: [
				[1:"1 minute"],
                [5:"5 minutes"],
				[10:"10 minutes"],
				[15:"15 minutes"],
				[30:"30 minutes"],
				[60:"1 hour"],
				[180:"3 hours"],
			], required: true, defaultValue: "10"
	input "seperate", "enum", title: "Cycle each light individually, or all together?", required: true, multiple: false, defaultValue: "individual", options: [
				[individual:"Individual"],
				[combined:"Combined"],
			]
    input "holidayalgo", "enum", title: "Color selection", required: true, multiple: false, defaultValue: "closest", submitOnChange: true, options: [
    		[closest:"Closest Holiday"],
            [closestwgo:"Next Holiday (with linger)"]
    	]
        if(holidayalgo == "closestwgo") {
            input "lingerdays", "number", title: "Days to linger after the holiday", required: true, defaultValue: 0
        }
    

	}

}
}
def allHolidayList() {
    return [
    [name: "New Years Day", day: '01/01', colors: ['White', 'Red', 'Pink', 'Purple'] ],
    [name: "Valentine's Day", day: '02/14', colors: ["Red", "Pink", "Rasberry", "Purple", "Indigo"] ],
    [name: "Presidents Day", day: '02/16', colors: ["Red", "White", "Blue" ] ],
    [name: "St. Patrick's Day", day: '03/17', colors: ["Green", "Orange"] ],
    [name: "Easter", day: '04/16', colors: [ 'Pink', 'Turquoise', 'Aqua' ] ],
    [name: "Mothers Day", day: '05/10', colors: ['Red', 'Pink'] ],
    [name: "Memorial Day", day: '05/25', colors: ["Red", "White", "Blue" ] ],
    [name: "Fathers Day", day: '06/21', , colors: ["Blue", "Navy Blue"] ],
    [name: "Independence Day", day: '07/04', colors: ["Red", "White", "Blue" ] ],
    [name: "Labor Day", day: '09/07', colors: ["Red", "White", "Blue" ] ],
    [name: "Halloween", day: '10/31', colors: ['Orange', 'Safety Orange' ] ],
    [name: "Veterans Day", day: '11/11', colors: ["Red", "White", "Blue" ] ],
    [name: "Thanksgiving", day: '11/26', colors: ['Orange', 'Safety Orange' ] ],
    [name: "Christmas Day", day: '12/25', colors: ["Red", "Green"] ]
	]

}
def holidayList() {
	return allHolidayList().findAll {holidays.contains(it.name)}
}

def holidayNames() {
    allHolidayList().name
}

def holidayTimestamps()  {
    def today = new Date()
	def this_year = today.format('Y')
	def last_year = (today - 365 ).format('Y')
	def next_year = (today + 365 ).format('Y')
	def timestamps = [:]
    holidayList().each {
        timestamps[Date.parse("${it.day}/${last_year} 23:59:59")] = it.name
        timestamps[Date.parse("${it.day}/${this_year} 23:59:59")] = it.name
        timestamps[Date.parse("${it.day}/${next_year} 23:59:59")] = it.name
    }
    return timestamps.sort()
}
def closestWithoutGO(buffer=0) {
    def today = new Date()
    today = today - buffer
    def target = today.getTime()
    def last = null
    def diff = null
     holidayTimestamps().any { k, v ->
        if (k > target) {
            last = v
            diff = k
            return true
        }
        return false
    }
    if ((maxdays == -1) || ( diff < ( maxdays  * 86400000) ))
    	return last
    else
    	return null
}
def closest() {
    def today = new Date()
    def last = null
    def distance = 99999999999999
     holidayTimestamps().each { k, v ->
        def d = k - today.getTime()
        if (d.abs() < distance) {
            distance = d.abs()
            last = v
        }
    }
    if ((maxdays == -1) || ( distance < ( maxdays  * 86400000) ))
    	return last
    else
    	return null
}


def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	schedule("0 0/${settings.cycletime} * 1/1 * ? *",changeHandler)
    state.colorOffset=0
}

def changeHandler(evt) {
        
    if (lights)
    {
    	def colors = []
        def curHoliday
        if(forceholiday) {
        	curHoliday = forceholiday
        }
        else 
        {
    		switch(holidayalgo) {
     	   		case 'closest':
                	curHoliday = closest()
            		break;
            	case 'closestwgo':
                	curHoliday = closestWithoutGO(lingerdays)
            		break;
 			}
        }
        log.debug curHoliday
        if (!curHoliday) {
        	log.debug "No holiday around..."
        	return false;
           }
        colors = allHolidayList().find {it.name == curHoliday }.colors
		def onLights = lights.findAll { light -> light.currentSwitch == "on"}
        def numberon = onLights.size();
        def numcolors = colors.size();
        log.debug "Offset: ${state.colorOffset}"
    	if (onLights.size() > 0) {
        	if (state.colorOffset >= numcolors ) {
            	state.colorOffset = 0
            }
			if (seperate == 'combined')
				sendcolor(onLights,colors[state.colorOffset])
            else {
            	log.debug "Colors: ${colors}"
           		for(def i=0;i<numberon;i++) {
                	sendcolor(onLights[i],colors[(state.colorOffset + i) % numcolors])
                }
            }
            state.colorOffset = state.colorOffset + 1
     	}
   	}
}

def sendcolor(lights,color)
{
log.debug "In send color"
	if (brightnessLevel<1) {
		brightnessLevel=1
	}
    else if (brightnessLevel>100) {
		brightnessLevel=100
	}

	def colorPallet = [
    	"White": [ hue: 0, saturation: 0],
    	"Daylight":  [hue: 53, saturation: 91],
    	"Soft White": [hue: 23, saturation: 56],
    	"Warm White": [hue: 20, saturation: 80],
    	"Navy Blue": [hue: 61, saturation: null],
    	"Blue": [hue: 65, saturation: null ],
    	"Green": [hue: 33, saturation: null ],
    	"Turquoise": [hue: 47, saturation: null ],
    	"Aqua": [hue: 50, saturation: null],
    	"Amber": [hue: 13, saturation: null],
    	"Yellow": [hue: 17, saturation: null],
    	"Safety Orange": [hue: 7, saturation: null],
    	"Orange": [hue: 10, saturation: null],
    	"Indigo": [hue: 73, saturation: null],
    	"Purple": [hue: 82, saturation: 100],
    	"Pink": [hue: 90.78, saturation: 67.84 ],
    	"Rasberry": [hue: 94 , saturation: null ],
    	"Red": [hue: 0, saturation: null ],
    	"Brick Red": [hue: 4, saturation: null ],
	]
	def newcolor = colorPallet."${color}"
    if(newcolor.saturation == null) newcolor.saturation = 100
    newcolor.level = brightnessLevel
	lights*.setColor(newcolor)
    log.debug "Setting Color = ${color} for: ${lights}"

}
