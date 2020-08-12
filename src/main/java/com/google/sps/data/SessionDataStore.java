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

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.google.gson.Gson;

import java.util.*;

public final class SessionDataStore {
    public static final String EMPTY_LIST = "[]";
    private final HttpServletRequest request;
    private final HttpSession currSession; 
    private final String sessionID; 

    public SessionDataStore(HttpServletRequest request){
        this.request = request;
        this.currSession = request.getSession();
        this.sessionID = this.currSession.getId();
    }

    /** Returns session ID of request. 
    */ 
    public String getSessionID(){
        return this.sessionID;
    }

    /** Changes fetchfields sessionAttribute to false.
    */ 
    public void setSessionAttributes(ArrayList<String> modFields){
        for (String field: modFields){
            this.currSession.setAttribute(field, false);
        }
    }

    /** If session has already fetched from servlet will return true.
    *   Else if it's first time to fetch from servlet will return false.
    */ 
    private boolean markFetch(String fieldToFetch){
        if (this.currSession.getAttribute(fieldToFetch) == null){
            this.currSession.setAttribute(fieldToFetch, true);
            return true;
        }else if (!(boolean) this.currSession.getAttribute(fieldToFetch)){
            this.currSession.setAttribute(fieldToFetch, true);
            return false;
        }else{
            return true;
        }
    }

    /** Stores Entity with Property Value with Session ID as id in DataStore for easy retrieval.
    */ 
    public void storeProperty(String EntityType, String PropertyName, String value){
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = datastore.beginTransaction();
        Entity Entity;
        try {
            Key ID = KeyFactory.createKey(EntityType, this.sessionID);
            Entity = datastore.get(ID);
        } catch (EntityNotFoundException e) {
            Entity = new Entity(EntityType, this.sessionID);
        }
        Entity.setUnindexedProperty(PropertyName, value);
        datastore.put(txn, Entity);
        txn.commit();
    }
    /** Retrieves the Property value of the Entity requested associated to the Session ID only if fetchAttribute is false. 
    */ 
    public String queryOnlyifFirstFetch(String fetchAttribute, String EntityType, String propertyToGet){
    boolean fetched = this.markFetch(fetchAttribute);
    if (!fetched){
        return this.fetchSessionEntity(EntityType, propertyToGet);
    }else{
        return EMPTY_LIST;
    }
    }

    /** Retrieves the Property value of the Entity requested. 
    */ 
    public String fetchSessionEntity(String EntityType, String propertyToGet){
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        try {
            Key ID = KeyFactory.createKey(EntityType, this.sessionID);
            Entity Entity = datastore.get(ID);
            return (String) Entity.getProperty(propertyToGet);
        } catch (EntityNotFoundException e) {
            return EMPTY_LIST;
        }
    }
    /** Creates an StoredRoute Entity that goes into GenRoute page.  
    */ 
    public void storeStoredRoute(){
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        TransactionOptions options = TransactionOptions.Builder.withXG(true);
        Transaction txn = datastore.beginTransaction(options);
        try{
            Key ID = KeyFactory.createKey("Route", this.sessionID);
            Entity Entity = datastore.get(ID);
            Entity newEntity = new Entity("StoredRoute");
            newEntity.setProperty("text", Entity.getProperty("text"));
            newEntity.setProperty("actual-route", Entity.getProperty("actual-route"));
            newEntity.setProperty("center-of-mass", Entity.getProperty("center-of-mass"));
            datastore.put(txn, newEntity);
        }catch (EntityNotFoundException e){}
        txn.commit();
    }

}