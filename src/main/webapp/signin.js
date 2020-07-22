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

var asyncRequest;    
function start(){
    try
    {
        asyncRequest = new XMLHttpRequest();
        asyncRequest.addEventListener("readystatechange", stateChange, false);
        asyncRequest.open('GET', '/userapi', true);  
        asyncRequest.send(null);
    }
    catch(exception)
   {
    alert("Request failed");
   }
}

function stateChange(){
if(asyncRequest.readyState == 4 && asyncRequest.status == 200)
    {
    var text = document.getElementById("content");         //  content is the id 
    text.innerHTML = asyncRequest.responseText;         //  div in HTML document
    }
}

window.addEventListener("load", start(), false);
