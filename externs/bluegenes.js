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
var bluegenes = {};
bluegenes.tools.chooselist.core.main = function() {};
bluegenes.tools.showlist.core.main = function() {};
bluegenes.tools.viewtable.core.main = function() {};
bluegenes.tools.idresolver.core.main = function() {};
bluegenes.tools.runtemplate.core.main = function() {};
bluegenes.tools.faketool.core.main = function() {};
bluegenes.tools.viewtable.core.preview = function() {};
bluegenes.tools.runtemplate.core.preview = function() {};
bluegenes.tools.enrichment.core.preview = function() {};
bluegenes.tools.enrichment.core.main = function() {};
bluegenes.tools.chooselistcompact.core.preview = function() {};
bluegenes.tools.chooselistcompact.core.main = function() {};


// Functions used by imjs
// TODO: we should create an IMJS externs file for all those cool CLJS hipsters out there.
var imjs = {};
imjs.prototype.Service = function() {};
imjs.prototype.Service.fetchLists = function() {};
imjs.prototype.Service.fetchTemplates = function() {};
imjs.prototype.Service.resolveIds = function() {};
imjs.prototype.Service.poll = function() {};
imjs.prototype.Service.query.count = function() {};

getAuthResponse = function() {};

jQuery.prototype.animate = function(properties, arg2, easing, complete) {};




var imtables = {};
imtables.loadTable = function() {};

var gapi = {};
gapi.auth2 = function() {};
gapi.auth2.init = function() {};
gapi.auth2 = function() {};
gapi.auth2.load = function() {};
gapi.prototype.auth2.load = function() {};

gapi.auth2.prototype.init = function() {};
api.prototype.auth2.init = function() {};
gapi.attachClickHandler = function() {};
gapi.auth2.attachClickHandler = function() {};
gapi.prototype.auth2.attachClickHandler = function() {};
gapi.auth2.prototype.attachClickHandler = function() {};
// attachClickHandler
