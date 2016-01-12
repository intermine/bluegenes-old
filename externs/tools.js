// This project makes use of Google's Closure Compiler advanced compilation which mangles
// function names. Native tools are constructed from the window using their namespace and
// therefore must be protected.
//
// Example:
//
// ourproject.tools.toolone.core.main is fetched using
// (-> ourproject.tools (aget "toolone") (aget "core") (aget "main))
//
// The above will fail if we don't save it from Google's liberal facelift.


// Native tools
// var bluegenes = {};
// bluegenes.tools.chooselist.core.main = function() {};


// Functions used by imjs
// TODO: we should create an IMJS externs file for all those cool CLJS hipsters out there.
var imjs = {};
imjs.prototype.Service = function() {};
imjs.prototype.Service.fetchLists = function() {};
