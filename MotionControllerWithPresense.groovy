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
definition(
	name:		"Motion Zone Presense",
	namespace:	"jlkSpace",
	author:		"Jeff Knox",
	singleInstance: true,
	installOnOpen: true,
	description: "Motion zone controller with cooldown.",
	category:	"Convenience",
	iconUrl:	"",
	iconX2Url:	"")

preferences {
    page(name:"mainPage")
}

def mainPage() {
	dynamicPage(name:"mainPage",title:"Motion Zone with Presense",install:true,uninstall:true) {
		section ("Manage motion zones") {
			app(name: "childApps", appName: "Motion Zone Presense (Child App)", namespace: "jlkSpace", title: "Add New Motion Zone Presense Helper", multiple: true)
		}
	}
}

def devicePage() {
}

void installed() {
   initialize()
}

void updated() {
   initialize()
}

void initialize() {
}
