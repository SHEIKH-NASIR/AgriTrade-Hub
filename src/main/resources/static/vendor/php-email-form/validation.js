document.addEventListener("DOMContentLoaded", function () {
  const form = document.querySelector('.php-email-form');

  form.addEventListener('submit', function (e) {
    e.preventDefault(); // Prevent default form submission

    const thisForm = this;

    // Show loading
    thisForm.querySelector('.loading').classList.add('d-block');
    thisForm.querySelector('.error-message').classList.remove('d-block');
    thisForm.querySelector('.sent-message').classList.remove('d-block');

    const formData = new FormData(thisForm);

    fetch(thisForm.getAttribute('action'), {
      method: 'POST',
      body: formData,
      headers: {
        'X-Requested-With': 'XMLHttpRequest'
      }
    })
    .then(response => {
      thisForm.querySelector('.loading').classList.remove('d-block');
      if (response.ok) {
        return response.text();
      } else {
        throw new Error(`Error ${response.status}: ${response.statusText}`);
      }
    })
	.then(data => {
	  if (data.trim().toLowerCase().startsWith('success')) {
	    /*thisForm.querySelector('.sent-message').innerHTML = data;*/
	    thisForm.querySelector('.sent-message').classList.add('d-block');
	    thisForm.reset();
	  } else {
	    throw new Error(data);
	  }
	})
    .catch(error => {
      thisForm.querySelector('.error-message').innerHTML = error.message;
      thisForm.querySelector('.error-message').classList.add('d-block');
    });
  });
});
