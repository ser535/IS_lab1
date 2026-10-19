const $ = selector => document.querySelector(selector);
const state = {
    page: 0,
    filters: {},
    section: 'routes',
    coordinates: [],
    locations: [],
    editId: null,
    version: null,
    detailId: null,
    operation: null,
    deletion: null,
};
let session;
let refreshing = false;
const sortFields = {
    id: 'ID',
    name: 'Название',
    creationDate: 'Дата создания',
    fromName: 'Название начала',
    toName: 'Название конца',
    distance: 'Расстояние',
    rating: 'Рейтинг',
};

const REFRESH_INTERVAL_MS = 2000;
const LONG_MIN = -9223372036854775808n;
const LONG_MAX = 9223372036854775807n;

function escapeHtml(value) {
    const entities = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;',
    };

    return String(value ?? '—').replace(/[&<>"']/g, character => entities[character]);
}

function locationText(location) {
    if (!location) {
        return '—';
    }

    return `#${location.id} ${location.name} (${location.x}; ${location.y}; ${location.z})`;
}

function coordinatesText(coordinates) {
    return `#${coordinates.id} (${coordinates.x}; ${coordinates.y})`;
}

async function api(path, options = {}) {
    const headers = { ...options.headers };
    if (options.body) {
        headers['Content-Type'] = 'application/json';
    }
    if (session) {
        headers[session.headerName] = session.token;
    }
    const response = await fetch('/api' + path, { ...options, headers });
    if (response.status === 401) {
        window.location.href = '/login';
        throw new Error('Войдите в систему');
    }
    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        const defaultMessage = response.status === 403
            ? 'Сессия истекла. Перезагрузите страницу.'
            : 'Ошибка запроса: ' + response.status;
        throw new Error(error.message || defaultMessage);
    }
    return response.status === 204 ? null : response.json();
}

function renderRouteRow(route) {
    return `
        <tr>
            <td>${route.id}</td>
            <td>${escapeHtml(route.name)}</td>
            <td>${escapeHtml(coordinatesText(route.coordinates))}</td>
            <td>${escapeHtml(route.creationDate)}</td>
            <td>${escapeHtml(locationText(route.from))}</td>
            <td>${escapeHtml(locationText(route.to))}</td>
            <td>${escapeHtml(route.distance)}</td>
            <td>${route.rating}</td>
            <td class="actions">
                <button data-view="${route.id}">Открыть</button>
                <button data-edit="${route.id}">Изменить</button>
                <button data-delete-route="${route.id}">Удалить</button>
            </td>
        </tr>
    `;
}

function renderRouteTable(routes) {
    if (!routes.length) {
        return '<p>Маршруты не найдены.</p>';
    }

    return `
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Название</th>
                    <th>Координаты</th>
                    <th>Дата создания</th>
                    <th>Начало</th>
                    <th>Конец</th>
                    <th>Расстояние</th>
                    <th>Рейтинг</th>
                    <th>Действия</th>
                </tr>
            </thead>
            <tbody>${routes.map(renderRouteRow).join('')}</tbody>
        </table>
    `;
}

async function loadRoutes() {
    const query = new URLSearchParams({ ...state.filters, page: state.page });
    const result = await api('/routes?' + query);
    state.page = result.page;
    $('#route-table').innerHTML = renderRouteTable(result.items);
    $('#pagination').textContent = `Страница ${result.page + 1} из ${Math.max(1, Math.ceil(result.total / result.size))}. Всего: ${result.total}`;
    $('#previous').disabled = result.page === 0;
    $('#next').disabled = (result.page + 1) * result.size >= result.total;
}

function updateReferenceSelects() {
    for (const select of document.querySelectorAll('[data-reference]')) {
        // Не перестраиваем список, пока пользователь выбирает значение.
        if (document.activeElement === select) {
            continue;
        }

        const selected = select.value;
        const values = state[select.dataset.reference];
        const format = select.dataset.reference === 'locations' ? locationText : coordinatesText;
        const options = '<option value="">Не выбрано</option>' + values
            .map(value => `<option value="${value.id}">${escapeHtml(format(value))}</option>`)
            .join('');

        if (select.innerHTML !== options) {
            select.innerHTML = options;
            select.value = selected;
        }
    }
}

function renderCoordinatesTable() {
    const rows = state.coordinates.map(coordinates => `
        <tr>
            <td>${coordinates.id}</td>
            <td>${escapeHtml(coordinates.x)}</td>
            <td>${escapeHtml(coordinates.y)}</td>
            <td>
                <button data-delete-reference="coordinates" data-id="${coordinates.id}">Удалить</button>
            </td>
        </tr>
    `).join('');

    return `
        <table>
            <tr><th>ID</th><th>X</th><th>Y</th><th>Действия</th></tr>
            ${rows}
        </table>
    `;
}

function renderLocationsTable() {
    const rows = state.locations.map(location => `
        <tr>
            <td>${location.id}</td>
            <td>${escapeHtml(location.name)}</td>
            <td>${location.x}</td>
            <td>${location.y}</td>
            <td>${location.z}</td>
            <td>
                <button data-delete-reference="locations" data-id="${location.id}">Удалить</button>
            </td>
        </tr>
    `).join('');

    return `
        <table>
            <tr><th>ID</th><th>Название</th><th>X</th><th>Y</th><th>Z</th><th>Действия</th></tr>
            ${rows}
        </table>
    `;
}

async function loadReferences() {
    [state.coordinates, state.locations] = await Promise.all([
        api('/coordinates'),
        api('/locations'),
    ]);

    updateReferenceSelects();
    $('#coordinates-table').innerHTML = renderCoordinatesTable();
    $('#locations-table').innerHTML = renderLocationsTable();
}

async function refresh() {
    if (refreshing) {
        return;
    }
    refreshing = true;
    try {
        await loadReferences();
        if (state.section === 'routes') {
            await loadRoutes();
        }
        if (state.section === 'operations' && state.operation) {
            await runOperation();
        }
        if ($('#details-dialog').open) {
            await loadDetails();
        }
        $('#status').textContent = '';
    } catch (error) {
        $('#status').textContent = error.message;
    } finally {
        refreshing = false;
    }
}

async function editRoute(id = null) {
    const route = id ? await api('/routes/' + id) : null;
    await loadReferences();
    state.editId = id;
    state.version = route?.version ?? null;
    const form = $('#route-form');
    form.reset();
    if (route) {
        const values = {
            name: route.name,
            coordinatesId: route.coordinates.id,
            fromId: route.from.id,
            toId: route.to?.id ?? '',
            distance: route.distance ?? '',
            rating: route.rating,
        };

        for (const [name, value] of Object.entries(values)) {
            form.elements[name].value = value;
        }
    }

    $('#route-title').textContent = id ? `Изменение маршрута #${id}` : 'Новый маршрут';
    form.querySelector('.error').textContent = '';
    $('#route-dialog').showModal();
}

async function loadDetails() {
    try {
        const route = await api('/routes/' + state.detailId);
        const fields = [
            ['ID', route.id],
            ['Название', route.name],
            ['Координаты', coordinatesText(route.coordinates)],
            ['Дата создания', route.creationDate],
            ['Начало', locationText(route.from)],
            ['Конец', locationText(route.to)],
            ['Расстояние', route.distance],
            ['Рейтинг', route.rating],
        ];
        const details = fields
            .map(([label, value]) => `<dt><b>${label}</b></dt><dd>${escapeHtml(value)}</dd>`)
            .join('');
        $('#details').innerHTML = `<dl>${details}</dl>`;
        $('#details-dialog .error').textContent = '';
        $('#edit-details').disabled = false;
    } catch (error) {
        $('#details').textContent = '';
        $('#details-dialog .error').textContent = error.message;
        $('#edit-details').disabled = true;
    }
}

async function viewRoute(id) {
    state.detailId = id;
    await loadDetails();
    $('#details-dialog').showModal();
}

function newReference(type) {
    const form = $('#reference-form');
    form.reset();
    form.dataset.type = type;
    const location = type === 'locations';
    $('#reference-title').textContent = location ? 'Новая локация' : 'Новые координаты';
    $('#location-z').hidden = $('#location-name').hidden = !location;
    form.elements.z.required = location;
    for (const name of ['x', 'y', 'z']) {
        form.elements[name].type = location ? 'number' : 'text';
        form.elements[name].step = 'any';
        form.elements[name].removeAttribute('pattern');
        if (!location) {
            form.elements[name].pattern = '-?[0-9]+';
        }
    }
    form.querySelector('.error').textContent = '';
    $('#reference-dialog').showModal();
}

async function prepareDelete(type, id) {
    state.deletion = { type, id };
    let description;
    if (type === 'routes') {
        const route = await api('/routes/' + id);
        state.deletion.version = route.version;
        description = `Удалить маршрут #${id} «${route.name}»? Его координаты и локации также удалятся, если не используются другими маршрутами.`;
    } else {
        const objectName = type === 'locations' ? 'локацию' : 'координаты';
        description = `Удалить ${objectName} #${id} и ВСЕ связанные маршруты?`;
    }
    $('#delete-description').textContent = description;
    $('#delete-dialog .error').textContent = '';
    $('#delete-dialog').showModal();
}

async function runOperation() {
    try {
        const result = await api(state.operation);
        if (typeof result === 'number') {
            $('#operation-result').innerHTML = `<p>Количество: <b>${result}</b></p>`;
        } else {
            const routes = Array.isArray(result) ? result : [result];
            $('#operation-result').innerHTML = renderRouteTable(routes);
        }
    } catch (error) {
        $('#operation-result').textContent = error.message;
    }
}

function submit(formId, handler) {
    $(formId).addEventListener('submit', async event => {
        event.preventDefault();
        const form = event.currentTarget;
        const button = form.querySelector('button:not([type="button"])');
        const errorElement = form.querySelector('.error') || $('#status');
        errorElement.textContent = '';
        if (button) {
            button.disabled = true;
        }
        try {
            await handler(Object.fromEntries(new FormData(form)), form);
        } catch (error) {
            errorElement.textContent = error.message;
        } finally {
            if (button) {
                button.disabled = false;
            }
        }
    });
}

async function saveRoute(data) {
    if (!data.name.trim()) {
        throw new Error('Название не должно быть пустым');
    }
    if (data.distance && (BigInt(data.distance) < 2n || BigInt(data.distance) > LONG_MAX)) {
        throw new Error('Расстояние: от 2 до 9223372036854775807');
    }
    const body = {
        name: data.name,
        coordinatesId: Number(data.coordinatesId),
        fromId: Number(data.fromId),
        toId: data.toId ? Number(data.toId) : null,
        // Long передаём строкой, чтобы JavaScript не округлил большое число.
        distance: data.distance || null,
        rating: Number(data.rating),
        version: state.version,
    };
    const path = state.editId ? `/routes/${state.editId}` : '/routes';
    await api(path, {
        method: state.editId ? 'PUT' : 'POST',
        body: JSON.stringify(body),
    });
    $('#route-dialog').close();
    await refresh();
}

async function saveReference(data, form) {
    const type = form.dataset.type;
    let body;
    if (type === 'coordinates') {
        if (BigInt(data.x) > 509n || BigInt(data.x) < LONG_MIN) {
            throw new Error('X: от -9223372036854775808 до 509');
        }
        if (BigInt(data.y) <= -897n || BigInt(data.y) > LONG_MAX) {
            throw new Error('Y: от -896 до 9223372036854775807');
        }
        body = { x: data.x, y: data.y };
    } else {
        body = { name: data.name, x: Number(data.x), y: Number(data.y), z: Number(data.z) };
        if (![body.x, body.y, body.z].every(Number.isFinite) || !Number.isFinite(Math.fround(body.x))) {
            throw new Error('Координаты должны быть конечными числами в диапазоне Float для X и Double для Y, Z');
        }
    }
    const created = await api('/' + type, { method: 'POST', body: JSON.stringify(body) });
    $('#reference-dialog').close();
    await loadReferences();
    if ($('#route-dialog').open) {
        const form = $('#route-form');
        if (type === 'coordinates') {
            form.elements.coordinatesId.value = created.id;
        } else if (!form.elements.fromId.value) {
            form.elements.fromId.value = created.id;
        } else {
            form.elements.toId.value = created.id;
        }
    }
    await refresh();
}

async function applyFilters(data) {
    state.filters = { sort: data.sort, direction: data.direction, size: data.size };
    const filterCheckboxes = {
        name: 'useName',
        fromName: 'useFromName',
        toName: 'useToName',
    };

    for (const [field, checkbox] of Object.entries(filterCheckboxes)) {
        if (data[checkbox]) {
            state.filters[field] = data[field];
        }
    }
    state.page = 0;
    await loadRoutes();
}

async function runRatingOperation(data) {
    if (data.operation === 'above') {
        state.operation = `/routes/operations/above?rating=${data.rating}`;
    } else {
        const greater = data.operation === 'greater';
        state.operation = `/routes/operations/count?rating=${data.rating}&greater=${greater}`;
    }
    await runOperation();
}

async function runBetweenOperation(data) {
    const query = new URLSearchParams({
        from: data.from,
        to: data.to,
        sort: data.sort,
        direction: data.direction,
        longest: data.operation === 'longest',
    });
    state.operation = '/routes/operations/' + (data.operation === 'all' ? 'between' : 'extreme') + '?' + query;
    await runOperation();
}

async function confirmDelete(button) {
    button.disabled = true;
    try {
        const { type, id, version } = state.deletion;
        const query = type === 'routes' ? `?version=${version}` : '';
        await api(`/${type}/${id}${query}`, { method: 'DELETE' });
        $('#delete-dialog').close();
        await refresh();
    } catch (error) {
        $('#delete-dialog .error').textContent = error.message;
    } finally {
        button.disabled = false;
    }
}

async function handleClick(event) {
    const button = event.target.closest('button');
    if (!button) {
        return;
    }

    try {
        if (button.dataset.page) {
            state.section = button.dataset.page;
            for (const section of document.querySelectorAll('main > section')) {
                section.hidden = section.id !== state.section;
            }
            await refresh();
        }
        if (button.hasAttribute('data-close')) {
            button.closest('dialog').close();
        }
        if (button.dataset.new) {
            newReference(button.dataset.new);
        }
        if (button.dataset.view) {
            await viewRoute(Number(button.dataset.view));
        }
        if (button.dataset.edit) {
            await editRoute(Number(button.dataset.edit));
        }
        if (button.dataset.deleteRoute) {
            await prepareDelete('routes', Number(button.dataset.deleteRoute));
        }
        if (button.dataset.deleteReference) {
            await prepareDelete(button.dataset.deleteReference, Number(button.dataset.id));
        }

        switch (button.id) {
            case 'create-route':
                await editRoute();
                break;
            case 'edit-details':
                $('#details-dialog').close();
                await editRoute(state.detailId);
                break;
            case 'previous':
                state.page--;
                await loadRoutes();
                break;
            case 'next':
                state.page++;
                await loadRoutes();
                break;
            case 'reset-filters':
                $('#filter-form').reset();
                state.filters = {};
                state.page = 0;
                await loadRoutes();
                break;
            case 'confirm-delete':
                await confirmDelete(button);
                break;
            case 'logout':
                await fetch('/logout', {
                    method: 'POST',
                    headers: { [session.headerName]: session.token },
                });
                window.location.href = '/login?logout';
                break;
        }
    } catch (error) {
        $('#status').textContent = error.message;
    }
}

function bindEvents() {
    submit('#route-form', saveRoute);
    submit('#reference-form', saveReference);
    submit('#filter-form', applyFilters);
    submit('#find-form', data => viewRoute(Number(data.id)));
    submit('#rating-form', runRatingOperation);
    submit('#between-form', runBetweenOperation);
    document.addEventListener('click', handleClick);
}

async function init() {
    const sortOptions = Object.entries(sortFields)
        .map(([key, label]) => `<option value="${key}">${label}</option>`)
        .join('');

    for (const select of document.querySelectorAll('.sort-fields')) {
        select.innerHTML = sortOptions;
    }

    bindEvents();
    session = await api('/session');
    $('#username').textContent = session.username;
    await refresh();
    setInterval(refresh, REFRESH_INTERVAL_MS);
}

init().catch(error => {
    $('#status').textContent = error.message;
});
