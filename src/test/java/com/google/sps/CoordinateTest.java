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
public final class CoordinateTest {
    public static final String LABEL = "name";
    public static final Double X = 3.0;
    public static final Double Y = 4.0;
    public static final String SPECIES = "bird";
    public static final String URL = "iNaturalist.com/bird";
    public static final String NOTHING = "";

    @Test
    public void canConstructCoordinateOnlyLabel() {
        Coordinate point = new Coordinate(X, Y, LABEL);
        Assert.assertEquals(X, point.getX());
        Assert.assertEquals(Y, point.getY());
        Assert.assertEquals(LABEL, point.getLabel());
        Assert.assertEquals(NOTHING, point.getSpecies());
        Assert.assertEquals(NOTHING, point.getUrl());
    }

    @Test
    public void canConstructCoordinateSpeciesAndUrl() {
        Coordinate point = new Coordinate(X, Y, LABEL, SPECIES, URL);
        Assert.assertEquals(X, point.getX());
        Assert.assertEquals(Y, point.getY());
        Assert.assertEquals(LABEL, point.getLabel());
        Assert.assertEquals(SPECIES, point.getSpecies());
        Assert.assertEquals(URL, point.getUrl());
    }
}