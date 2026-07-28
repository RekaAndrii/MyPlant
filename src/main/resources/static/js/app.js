// Shared application bootstrap — loaded on every authenticated page.
// Requires jQuery to be loaded before this script.

// Redirect to login if any AJAX call is intercepted by Spring Security
// (session expired: server returns 200 with the login page body)
$(document).ajaxComplete(function (event, xhr) {
    if (xhr.responseURL && xhr.responseURL.indexOf('/login') !== -1) {
        window.location.href = '/login';
    }
});

// Toggle the collapsed navbar menu on mobile (Bootstrap 3 JS is not loaded).
$(document).on("click", "#navbarToggle", function () {
    $("#navbar").toggleClass("in");
});
