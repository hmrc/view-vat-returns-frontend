$(document).ready(function() {
  //initialise <details> polyfill from frontend toolkit
  GOVUK.details.init();
});

var mediaQueryList = window.matchMedia('print');

var currH1 = document.getElementsByTagName("h1").item(0);
var newH1 = 'Your VAT Return<span class="heading-secondary">' + currH1.innerText.substr(18) + '</span>';

var returnTotal = document.getElementsByClassName("heading-large").item(0);
var currReturnTotal = returnTotal.outerHTML;

mediaQueryList.addListener(function(mql) {
  if(mql.matches) {
    document.getElementsByTagName("h1").item(0).innerHTML = newH1;
    returnTotal.remove();
    document.getElementsByClassName("button").item(0).insertAdjacentHTML("afterend", currReturnTotal);
    document.insert;
  } else {
    location.reload();
  }
});
