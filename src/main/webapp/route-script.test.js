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

// mock the basic DOM of our webpage
document.body.innerHTML = 
    '<form action="/chosen-waypoints" id="select-points"></form>' +
    '<form action="/query" method="POST" id ="text-git statform">' +
    '<textarea name="text-input" id="text-input"></textarea>' +
    '<input type="submit" id="submit-text"/>' +
    '</form>';

describe('addNewLegendElem', function() {
    const addNewLegendElem = require('./route-script.js').addNewLegendElem;

    const mockParent = document.createElement('div');
    mockParent.setAttribute('id', 'mock-parent');
    const mockText = "test";

    it('should be a function', function() {
        expect(addNewLegendElem).toBeInstanceOf(Function);
    });

    it('should add a single p elem with given text to the DOM', function() {
        addNewLegendElem(mockParent, mockText);
        expect(mockParent.childNodes.length).toEqual(1);
        expect(mockParent.childNodes[0].tagName).toBe('P');
        expect(mockParent.childNodes[0].textContent).toBe(mockText);
    });
});

describe('createColorBoxElem', function() {
    const createColorBoxElem = require('./route-script.js').createColorBoxElem;
    const testColor = 'red';

    it('should be a function', function() {
        expect(createColorBoxElem).toBeInstanceOf(Function);
    });

    it('should create box with given color', function() {
        let box = createColorBoxElem(testColor);
        expect(box.tagName).toBe('DIV');
        expect(box.style.backgroundColor).toBe(testColor);
    });

    it('should create square of size 20px', function() {
        let box = createColorBoxElem(testColor);
        expect(box.style.width).toEqual(box.style.height);
        expect(box.style.width).toEqual('20px');
        expect(box.style.height).toEqual('20px');
    });
});

describe('createCheckBoxEl', function() {
    const createCheckBoxEl = require('./route-script.js').createCheckBoxEl;

    const testCoords = {x : 2, y : 2, label : 'flower'};
    const testLabel = 'flower';

    it('should be a function', function() {
        expect(createCheckBoxEl).toBeInstanceOf(Function);
    });

    it('should return a div element', function() {
        const checkbox = createCheckBoxEl(testCoords, testLabel);
        expect(checkbox.tagName).toEqual('DIV');
    });

    it('should return a div element with input and label child nodes', function() {
        const checkbox = createCheckBoxEl(testCoords, testLabel);
        expect(checkbox.childNodes.length).toEqual(2);
        expect(checkbox.childNodes[0].tagName).toEqual('INPUT');
        expect(checkbox.childNodes[1].tagName).toEqual('LABEL');
    });

    it('should return a div element with a child label containing the inputted label parameter', function() {
        const checkbox = createCheckBoxEl(testCoords, testLabel);
        expect(checkbox.childNodes[1].innerText).toEqual(testLabel);
    });
});