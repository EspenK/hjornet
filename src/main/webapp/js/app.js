'use strict';

(function () {
    function init() {
        let router = new Router([
            new Route('signin', 'signin.html'),
            new Route('signup', 'signup.html'),
            new Route('items', 'items.html', true),
            new Route('item', 'item.html'),
            new Route('newitem', 'newitem.html')
        ]);
    }

    init();
    updateNavUser();
}());
