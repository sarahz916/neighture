// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;
import com.google.gson.Gson;
import com.google.sps.Coordinate;
import java.util.*;

public final class WaypointsObject {
    private List<Coordinate> waypointList;
    private Coordinate center;
    private String labelSentence;

    /** Averages latitude and longititude of all waypoints and returns Coordinate of center. 
        Gets Coordinate labels together and returns as comma seperated string.
    */ 
    public WaypointsObject(ArrayList<Coordinate> waypoints){
        this.waypointList = waypoints;
        int num_pts = waypoints.size(); 
        Double avg_x = 0.0;
        Double avg_y = 0.0;
        StringBuilder labelSentencebuild = new StringBuilder();
        for (Coordinate pt: waypoints){
            avg_x += pt.getX();
            avg_y += pt.getY();
            labelSentencebuild.append(pt.getLabel());
            labelSentencebuild.append(", ");
        }
        avg_x = avg_x / num_pts;
        avg_y = avg_y / num_pts;
        this.center =  new Coordinate(avg_x, avg_y, "midpoint");
        this.labelSentence = labelSentencebuild.substring(0, labelSentencebuild.length() - 2);
    }

    public String getLabelSentence (){
        return this.labelSentence;
    }

    public Coordinate getCenter(){
        return this.center;
    }

    public List<Coordinate> getWaypointList(){
        return this.waypointList;
    }

    public String getJSONofWaypoints(){
        return (String) new Gson().toJson(this.waypointList);
    }


}