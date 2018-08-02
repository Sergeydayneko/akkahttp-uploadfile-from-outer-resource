$(function () {
  $('#submit').click(function (e) {
    e.preventDefault();

    let user = {
      login    : $('#inputLogin').val(),
      password : $('#inputPassword').val()
    };

    $.ajax({
      url: 'login',
      type: 'POST',
      data: JSON.stringify(user),
      processData: false,
      contentType: "application/json",
      success: function( respond, textStatus, jqXHR ){
        window.location = ("/main")
      },
      error: function(jqXHR, textStatus, errorThrown){
        console.log('ОШИБКИ AJAX запроса: ' + textStatus );
      }
    });
  })
});
