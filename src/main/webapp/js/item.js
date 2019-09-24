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

async function handleNewItem(form) {
    let item = {
        'title': form.title.value,
        'price': parseInt(form.price.value, 10),
        'description': form.description.value
    };
    let response = await fetch_secure('api/item/create', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(item)
    });
    if (response.status !== 200) {
        let data = await response.json();
        console.log(data.message)
    }

    location.href = '#items';
}

function showItems() {
    waitForElementFirst('itemsContainer', 50, _showItems);
}

async function _showItems(container) {
    container.innerHTML = '';

    let result = await fetch('api/item/all');
    if (result.status !== 200) {
        console.log('could not get items');
        return;
    }

    let items = await result.json();
    items.forEach(showItem);

    function showItem(item) {
        let section = document.createElement('section');
        section.onclick = function () {
            showItem(item);
        };

        section.innerHTML = `
        <img alt="${item.title}" src="https://www.thetimes.co.uk/imageserver/image/methode%2Ftimes%2Fprod%2Fweb%2Fbin%2F696b3456-abbf-11e8-aa49-f23497b9293e.jpg?crop=3333%2C1875%2C76%2C340&resize=685">
        <div class="padded left">${item.owner.postalArea} ${item.owner.postalCode}</div>
        <div class="padded right price">${item.price} kr</div>
        <div class="padded clear title">${item.title}</div>
        `;
        container.appendChild(section);
    }
}