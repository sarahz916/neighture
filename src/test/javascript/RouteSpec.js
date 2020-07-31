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

describe("Route Script Spec", function() {
    // simple test to check if the testing environment is working
    it("finds true to be true", function() {
        expect(true).toBe(true);
    });

    describe("DOM manipulation", function() {
        const jsdom = require("jsdom");
        const { JSDOM } = jsdom;
        const dom = new JSDOM(`<!DOCTYPE html><p>Test DOM</p>`);

        it("can create new legend elem", function() {
            const parent = dom.window.document.createElement('div');
            const text = 'test';
            addNewLegendElem(parent, text);
            expect(parent.childNodes.length).toBe(1);
            expect(parent.childNodes[0].tagName).toBe('p');
            expect(parent.childNodes[0].textContent).toBe(text);
        });
    });
});