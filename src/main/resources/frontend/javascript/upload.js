$(function () {
  $(`.logout-button`).on('click', function () {
    $.post( "logout", function() {
      window.location.replace("login");
    });
  })
});