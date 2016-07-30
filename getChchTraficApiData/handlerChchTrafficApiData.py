#!/usr/bin/python
import json
from datetime import datetime as dt

json_test_file = "govHachGeoJson.txt"

def getChchJsonData():
   json_text = open(json_test_file, 'r').read()

   return json.loads(json_text)

def getRoadClosureToday(json_data):
   roadClosureFeatures = []

   for feature in json_data["features"]:
      if "Road Closure" == feature["properties"]["roadclosurestatus"]:
         startdate = dt.strptime(feature["properties"]["startdate"], "%Y-%m-%d")
         enddate   = dt.strptime(feature["properties"]["enddate"], "%Y-%m-%d")
         today     = dt.today()
         if today > startdate and today < enddate:
            roadClosureFeatures.append(feature)
   return roadClosureFeatures

def main():
   json_data = getChchJsonData()
   roadClosures = getRoadClosureToday(json_data)
   print len(roadClosures)

main()
