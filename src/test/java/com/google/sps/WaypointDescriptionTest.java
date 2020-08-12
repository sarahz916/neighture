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
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Assert;

@RunWith(JUnit4.class)
public final class WaypointDescriptionTest {
  public static final String LONG_LABEL = "great blue heron";
  public static final String LONG_QUERY = "great%20blue%20heron";
  public static final String GREAT_BLUE = "great blue";
  public static final String HERON = "heron";
  public static final String DEFAULT_TEXT = "";  
  public static final int AMOUNT = 3;
  public static final int DEFAULT_AMOUNT = 5;

  @Test
  public void constructFullWaypointDescription() {
    WaypointDescription description = new WaypointDescription(AMOUNT, HERON);
    Assert.assertEquals(AMOUNT, description.getMaxAmount());
    Assert.assertEquals(HERON, description.getFeature());
    Assert.assertEquals(HERON, description.getQuery());
    Assert.assertTrue(description.hasFeature());
    Assert.assertTrue(description.maxAmountWasSet());
  }

  @Test
  public void constructWaypointDescriptionAmount() {
    WaypointDescription description = new WaypointDescription(AMOUNT);
    Assert.assertEquals(AMOUNT, description.getMaxAmount());
    Assert.assertEquals(DEFAULT_TEXT, description.getFeature());
    Assert.assertEquals(DEFAULT_TEXT, description.getQuery());
    Assert.assertFalse(description.hasFeature());
    Assert.assertTrue(description.maxAmountWasSet());
  }

  @Test
  public void constructWaypointDescriptionFeature() {
    WaypointDescription description = new WaypointDescription(HERON);
    Assert.assertEquals(DEFAULT_AMOUNT, description.getMaxAmount());
    Assert.assertEquals(HERON, description.getQuery());
    Assert.assertEquals(HERON, description.getFeature());
    Assert.assertTrue(description.hasFeature());
    Assert.assertFalse(description.maxAmountWasSet());
  }

  @Test
  public void constructEmptyWaypointDescription() {
    WaypointDescription description = new WaypointDescription();
    Assert.assertEquals(DEFAULT_AMOUNT, description.getMaxAmount());
    Assert.assertEquals(DEFAULT_TEXT, description.getQuery());
    Assert.assertEquals(DEFAULT_TEXT, description.getFeature());
    Assert.assertFalse(description.hasFeature());
    Assert.assertFalse(description.maxAmountWasSet());
  }

  @Test
  public void changeMaxAmountFromDefault() { 
    WaypointDescription description = new WaypointDescription();
    Assert.assertEquals(DEFAULT_AMOUNT, description.getMaxAmount());
    description.setMaxAmount(AMOUNT);
    Assert.assertEquals(AMOUNT, description.getMaxAmount());
  }

  @Test
  public void addFeatures() { 
    WaypointDescription description = new WaypointDescription(GREAT_BLUE);
    description.addFeature(HERON);
    Assert.assertEquals(LONG_LABEL, description.getFeature());
    Assert.assertEquals(LONG_QUERY, description.getQuery());
  }
}