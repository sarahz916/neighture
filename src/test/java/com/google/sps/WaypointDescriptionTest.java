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
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Assert;

@RunWith(JUnit4.class)
public final class WaypointDescriptionTest {
  public static final String LABEL = "3 daisy, clover";
  public static final String DEFAULT_LABEL = "UNLABELED";  
  public static final int AMOUNT = 3;
  public static final int DEFAULT_AMOUNT = 5;
  public static final ArrayList<String> SOME_FEATURES = new ArrayList<String>(Arrays.asList("daisy", "clover"));
  public static final ArrayList<String> NO_FEATURES = new ArrayList<String>();

  @Test
  public void constructFullWaypointDescription() {
    WaypointDescription description = new WaypointDescription(AMOUNT, SOME_FEATURES);
    Assert.assertEquals(AMOUNT, description.getAmount());
    Assert.assertEquals(DEFAULT_LABEL, description.getLabel());
    Assert.assertEquals(SOME_FEATURES, description.getFeatures());
    Assert.assertFalse(description.hasLabel());
    Assert.assertTrue(description.hasFeatures());
    Assert.assertTrue(description.maxAmountWasSet());
  }

  @Test
  public void constructWaypointDescriptionAmount() {
    WaypointDescription description = new WaypointDescription(AMOUNT);
    Assert.assertEquals(AMOUNT, description.getAmount());
    Assert.assertEquals(DEFAULT_LABEL, description.getLabel());
    Assert.assertEquals(NO_FEATURES, description.getFeatures());
    Assert.assertFalse(description.hasLabel());
    Assert.assertFalse(description.hasFeatures());
    Assert.assertTrue(description.maxAmountWasSet());
  }
  
  @Test
  public void constructWaypointDescriptionFeatures() {
    WaypointDescription description = new WaypointDescription(SOME_FEATURES);
    Assert.assertEquals(DEFAULT_AMOUNT, description.getAmount());
    Assert.assertEquals(DEFAULT_LABEL, description.getLabel());
    Assert.assertEquals(SOME_FEATURES, description.getFeatures());
    Assert.assertFalse(description.hasLabel());
    Assert.assertTrue(description.hasFeatures());
    Assert.assertFalse(description.maxAmountWasSet());
  }

  @Test
  public void constructEmptyWaypointDescription() {
    WaypointDescription description = new WaypointDescription();
    Assert.assertEquals(DEFAULT_AMOUNT, description.getAmount());
    Assert.assertEquals(DEFAULT_LABEL, description.getLabel());
    Assert.assertEquals(NO_FEATURES, description.getFeatures());
    Assert.assertFalse(description.hasLabel());
    Assert.assertFalse(description.hasFeatures());
    Assert.assertFalse(description.maxAmountWasSet());
  }

  @Test
  public void successfullyCreateLabel() {
    WaypointDescription description = new WaypointDescription(AMOUNT, SOME_FEATURES);
    description.createLabel();
    Assert.assertEquals(LABEL, description.getLabel());
    Assert.assertTrue(description.hasLabel());
  }

  @Test
  public void createLabelFail() { // Fails because there are no features
    WaypointDescription description = new WaypointDescription(AMOUNT);
    description.createLabel();
    Assert.assertEquals(DEFAULT_LABEL, description.getLabel());
    Assert.assertFalse(description.hasLabel());
  }

  // TODO: 
  // Add tests + code for adding/changing amount, label, and features
}