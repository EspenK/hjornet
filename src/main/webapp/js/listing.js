let listings = [];

function handleNewListing(form) {
    let listing = {
        'title': form.title.value,
        'price': form.price.value,
        'image': form.image.value,
        'description': form.description.value
    };
    listings.push(listing);
    localStorage.setItem('listings', JSON.stringify(listings));
    location.href = '#listings';
}

function waitForElementFirst(element, time, func, arg) {
    let container;
    if (document.getElementById(element) != null) {
        container = document.getElementById(element);
        setTimeout(function () {
            func(container, arg);
        }, time);
    } else {
        setTimeout(function () {
            waitForElementFirst(element, time, func, arg);
        }, time);
    }
}

function showListings() {
    waitForElementFirst('listingsContainer', 50, _showListings);
}

function _showListings(container) {
    container.innerHTML = '';

    listings.forEach(showListing);

    function showListing(listing) {
        let section = document.createElement('section');
        section.onclick = function () {
            showListingFull(listing);
        };

        section.innerHTML = `
        <img alt="${listing.title}" src="${listing.image}">
        <div class="padded left postalArea">Oslo</div>
        <div class="padded right price">${listing.price} kr</div>
        <div class="padded clear title">${listing.title}</div>
        `;
        container.appendChild(section);
    }
}
