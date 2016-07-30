#!/usr/bin/python
import os
import json
import geojson
import argparse
import polyline
from shapely.geometry import shape, Point
from datetime import datetime as dt

json_test_file = os.path.join(os.path.dirname(os.path.abspath(__file__)),"govHachGeoJson.txt")

def handleArgs():
    parser = argparse.ArgumentParser(description="Gets Google Maps polyline and checks if any roadclosures are on the way.")

    parser.add_argument('--p','-polyline', dest = "polyline", required=True, help="file Google Maps polyline")

    args = parser.parse_args()

    cnf = {'polyline_file' : args.polyline,
           'json_file'     : json_test_file}

    return cnf

def getChchJsonData(json_file):
   json_text = open(json_file, 'r').read()

   return geojson.loads(json_text)

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

def getClosedArea(closed_road):
   for area in closed_road['geometry']['geometries']:
      if area["properties"]["type"] == "extent" and area["properties"]["model"] == "RoadClosure":
         return area

   return None

def getClosedFocus(closed_road):
   for area in closed_road['geometry']['geometries']:
      if area["properties"]["type"] == "focus" and area["properties"]["model"] == "RoadClosure":
         return area

   return None


def isFocusDuplicated(blocks, lat, lon):
   for block in blocks:
      if block['lat'] == lat and block['lng'] == lon:
         return True   

   return False

def getAllRoadsClosedOnTheRoute(roadClosures, route):
   closed_blocks = []

   for step in route:
      point = Point(step)

      for closed_road in roadClosures:
         closed_area = getClosedArea(closed_road)
         focus_point = getClosedFocus(closed_road)
         
         if closed_area is not None and focus_point is not None:
            polygon = shape(closed_area)

            if polygon.contains(point):
               #print(json.dumps(closed_road, sort_keys=True, indent=4))

               if isFocusDuplicated(closed_blocks, focus_point["coordinates"][0], focus_point["coordinates"][1]):
                  continue
               closed_blocks.append({'address'     : closed_road["properties"]["address"],
                                     'jobtype'     : closed_road["properties"]["jobtype"],
                                     'description' : closed_road["properties"]["publicdescription"],
                                     'lat'         : focus_point["coordinates"][0],
                                     'lng'         : focus_point["coordinates"][1]})
   return closed_blocks

def main():
   #test_pline = "fe_iGg{i|_@EIIMW[QUKIWWyIyH_]gZkVeTwAqA{BoBGGGIQSIKKO"
   #test_pline = "prwhGmer|_@oF?CjVHBDF@H`G?HA"
   
   cnf = handleArgs();
   pline = open(cnf['polyline_file'], 'r').read().replace("\\\\", "\\")

   json_data = getChchJsonData(cnf['json_file'])

   roadClosures = getRoadClosureToday(json_data)
   
   route = polyline.decode(pline)
   
   closed_blocks = getAllRoadsClosedOnTheRoute(roadClosures, route)
   print json.dumps(closed_blocks, sort_keys=True, indent=4)

main()
