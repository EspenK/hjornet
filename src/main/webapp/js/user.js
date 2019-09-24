async function handleSignUp(form) {
    let user = {
        'firstName': form.firstName.value,
        'lastName': form.lastName.value,
        'email': form.email.value,
        'password': form.password.value,
        'phoneNumber': form.phoneNumber.value,
        'streetAddress': form.streetAddress.value,
        'postalArea': form.postalArea.value,
        'postalCode': form.postalCode.value
    };
    let response = await fetch('api/auth/create', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(user)
    });

    if (response.status !== 200) {
        let data = await response.json();
        console.log(data.message)
    } else {
        location.href = '#signin';
    }
}

async function handleSignIn(form) {
    let email = form.email.value;
    let password = form.password.value;

    let response = await fetch('api/auth/login?email=' + email + '&password=' + password, {
        method: 'GET'
    });

    if (response.status !== 200) {
        let data = await response.json();
        console.log(data.message)
    } else {
        let data = await response.json();
        localStorage.setItem('bearer', data.token);
        await updateNavUser();
        location.href = '#items'
    }
}

async function handleSignOut() {
    if (isBearerCached()) {
        let response = await fetch_secure('api/auth/logout', {method: 'GET'});
        if (response.status !== 200) {
            console.log('user was not logged out');
        } else {
            localStorage.removeItem('bearer');
            await updateNavUser();
            location.href = '#items';
        }
    }
}

function isBearerCached() {
    return localStorage.getItem('bearer') !== null;
}

async function getCurrentUser() {
    if (isBearerCached()) {
        let response = await fetch_secure('api/auth/currentuser', {method: 'GET'});
        if (response.status !== 200) {
            return null;
        } else {
            return await response.json();
        }
    } else {
        return null;
    }
}

async function updateNavUser() {
    if (isBearerCached()) {
        let currentUser = await getCurrentUser();
        document.getElementById('navUser').innerHTML = `${currentUser.firstName} ${currentUser.lastName} | <a href="#" onclick="handleSignOut()">Sign out</a>`;
    } else {
        document.getElementById('navUser').innerHTML = '<a href="#signin">Sign in</a> or <a href="#signup">Sign up</a>'
    }
}

async function fetch_secure(input, init) {
    if (!isBearerCached()) {
        return null;
    }

    init = init || {};
    init.headers = init.headers || {};

    init.withCredentials = true;
    init.credentials = 'include';
    init.headers.Authorization = 'Bearer ' + localStorage.bearer;

    return await fetch(input, init);
}
