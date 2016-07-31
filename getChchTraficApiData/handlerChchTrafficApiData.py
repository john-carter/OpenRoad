#!/usr/bin/python
import os
import json
import geojson
import argparse
import polyline
from shapely.geometry import shape, Point, LineString
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
   areas = []
   for area in closed_road['geometry']['geometries']:
      if area["properties"]["type"] == "extent" and area["properties"]["model"] == "RoadClosure":
         areas.append(area)

   return areas

def getClosedFocus(closed_road):
   focuses = []
   for area in closed_road['geometry']['geometries']:
      if area["properties"]["type"] == "focus" and area["properties"]["model"] == "RoadClosure":
         focuses.append(area)

   return focuses


def isFocusDuplicated(blocks, lat, lon):
   for block in blocks:
      if block['lat'] == lat and block['lng'] == lon:
         return True   

   return False

def getAllRoadsClosedOnTheRoute(roadClosures, route):
   closed_blocks = []

   for i in range(1, len(route)):
      leg = LineString([route[i-1], route[i]])
   
      for closed_road in roadClosures:
         closed_areas = getClosedArea(closed_road)
         focus_points = getClosedFocus(closed_road)
         
         if closed_areas is not [] and focus_points is not None:
            for sub_area in closed_areas:
               polygon = shape(sub_area)

               if polygon.intersects(leg):
               
                  for focus_point in focus_points:
                     
                     if isFocusDuplicated(closed_blocks, focus_point["coordinates"][0], focus_point["coordinates"][1]):
                        continue

                     #print(json.dumps(closed_road, sort_keys=True, indent=4))
                     
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

   #pline = "djuhGect|_@AqK?SiAAiB?eB?e@G_@QYYi@k@MK@oDAmA"
   #pline = "djuhG{bt|_@?mFxE?TARATEVKd@YxAqA^OXEfGAhJ?zJ@zABpIDFGVAR?|BAxCGDBv@@nLAhG?|@??yC?mH?kCAOTAzAAhLK|IIbJCjf@MbD?f@C`CG~OUxAEp@A`@Bl@N\\Nh@d@nAvAlCzCbAfAb@\\FHB?FQHSDGHA@C^AZ?H?hDnBFHFHVXnF|C"
   #pline = "djuhGsct|_@AcK?SiAAiB?eB?e@G_@QYYi@k@w@o@"

   json_data = getChchJsonData(cnf['json_file'])

   roadClosures = getRoadClosureToday(json_data)
   
   route = polyline.decode(pline)
   
   closed_blocks = getAllRoadsClosedOnTheRoute(roadClosures, route)
   print json.dumps(closed_blocks, sort_keys=True, indent=4)

main()
