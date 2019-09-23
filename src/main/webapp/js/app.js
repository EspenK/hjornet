'use strict';

(function () {
    function init() {
        let router = new Router([
            new Route('signin', 'signin.html'),
            new Route('signup', 'signup.html'),
            new Route('listings', 'listings.html', true),
            new Route('listing', 'listing.html'),
            new Route('newlisting', 'newlisting.html')
        ]);
    }

    init();
    updateNavUser();
}());
