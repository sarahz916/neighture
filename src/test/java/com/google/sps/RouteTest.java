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

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class RouteTest {
    public static final Point ORIGIN = new Point(0, 0);
    public static final Point ZERO_ONE = new Point(0, 1);
    public static final Point ONE_ONE = new Point(1, 1);
    public static final Point ONE_ZERO = new Point(1, 0);
    public static final ArrayList<Point> TRIANGLE = new ArrayList<Point>(Arrays.asList(ZERO_ONE, ONE_ONE, ONE_ZERO));

    @Test
    public void canConstructSimpleRouteObject() {
        Route route = new Route(ORIGIN, ORIGIN, TRIANGLE);
        Assert.assertEquals(ORIGIN, route.getStart());
        Assert.assertEquals(ORIGIN, route.getEnd());
        Assert.assertEquals(TRIANGLE, route.getWaypoints());
    }
}