$(document).ready(function() {
  //initialise <details> polyfill from frontend toolkit
  GOVUK.details.init();
});

var mediaQueryList = window.matchMedia('print');

var returnTotal = document.getElementsByClassName("heading-large").item(0);
var currReturnTotal = returnTotal.outerHTML;

mediaQueryList.addListener(function(mql) {
  if(mql.matches) {
    returnTotal.remove();
    document.getElementsByClassName("button").item(0).insertAdjacentHTML("afterend", currReturnTotal);
  } else {
    location.reload();
  }
});
