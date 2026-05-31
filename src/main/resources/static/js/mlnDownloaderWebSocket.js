/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */
import { deleteDownloaded, pauseOrResumeDownload, forceMerge, changeSpeedLimit } from "./mlnDownloaderFetch.js";

let downloads = new Map();

document.addEventListener("DOMContentLoaded", () => {
    connect();
    const container = document.getElementById('downloadsContainer');
});

const stompClient = new StompJs.Client({
    brokerURL: "ws://" + window.location.host + "/ws-connect"
});

const SUBSCRIBE_PREFIX = "/topic";

stompClient.onConnect = (frame) => {

    setConnected(true);

    console.log('Connected: ' + frame);

    stompClient.subscribe(`${SUBSCRIBE_PREFIX}/downloads`, (jsonString) => {
        subcriptionDownloads(jsonString);
    });

    stompClient.subscribe(`${SUBSCRIBE_PREFIX}/disc_info`, (jsonString) => {
        subcriptionDiscInfo(jsonString);
    });
};

function subcriptionDownloads(jsonString) {

    const data = JSON.parse(jsonString.body);

    const downloadsContainer = document.querySelector("#downloadsContainer");


    if (Array.isArray(data) && data.length === 0) {
        downloadsContainer.innerHTML = "";
        downloads = new Map();
    }


    const ids = new Set(data.map(x => `download_${x.id}`));

    for (const key of downloads.keys()) {
        if (!ids.has(key)) {
            downloads.delete(key);
            document.querySelector(`#${key}`).remove();
        }
    }


    data.forEach((download, index) => {

        const downloadOnMap = downloads.get("download_" + download.id)

        if (downloadOnMap) {
            updateCard(download)
        } else {
            downloads.set("download_" + download.id, download);

            downloadsContainer.insertAdjacentHTML(
                "beforeend",
                createDownloadCard(download, index)
            );

            const actionBtn = document.querySelector(`[data-file="actionBtn_${download.id}"]`);
            const delBtn = document.querySelector(`[data-file="delBtn_${download.id}"]`);
            const speedLimit = document.querySelector(`#speedLimit_${download.id}`);


            actionBtn.addEventListener("click", (e) => {
                const btn = e.target;
                const id = btn.dataset.file.split("_")[1];
                pauseOrResumeDownload(id)
            })

            delBtn.addEventListener("click", (e) => {
                const id = e.target.dataset.file.split("_")[1];
                deleteDownloaded(id, downloads)
            })

            speedLimit.addEventListener("change", () => {
                changeSpeedLimit(download.id, speedLimit.value)
            })
        }
    });

}


function subcriptionDiscInfo(jsonString) {

    const discInfo = JSON.parse(jsonString.body)

    const spaceInfoString = `${getBytesToGB(discInfo.freeSpace)} / ${getBytesToGB(discInfo.totalSpace)} GB`;

    const smallInfoText = document.querySelector(`[data-file="disc-info"]`);

    smallInfoText.textContent = spaceInfoString;

    const infoDiscDiv = document.querySelector("#infoDiscDiv");


    if (!discInfo.spaceSuficient) {
        infoDiscDiv.classList.add("blink-bg-danger");
        smallInfoText.innerHTML += `<br><span class="fw-bold">¡¡ NO HAY SUFICIENTE ESPACIO PARA DESCARGAR TODOS LOS ARCHIVOS, TODAS LAS DESCARGAS PARADAS HASTA SOLUCIONARLO !!</span>`;
    } else {
        infoDiscDiv.classList.remove("blink-bg-danger");
        infoDiscDiv.classList.remove("blink-bg-warning");
    }


    if (discInfo.freeSpace < (discInfo.totalSpace * 0.1) && discInfo.freeSpace > (discInfo.totalSpace * 0.05)) {
        infoDiscDiv.classList.add("blink-bg-warning");
        smallInfoText.innerHTML += `<br><span class="fw-bold">¡¡ DISCO POR DEBAJO DE UN 10% DE SU CAPACIDAD !!</span>`;
    } else if (discInfo.freeSpace < (discInfo.totalSpace * 0.05)) {
        infoDiscDiv.classList.remove("blink-bg-warning");
        infoDiscDiv.classList.add("blink-bg-danger");
        smallInfoText.innerHTML += `<br><span class="fw-bold">¡¡ DISCO POR DEBAJO DE UN 5% DE SU CAPACIDAD !!</span>`;
    }
}

function updateCard(download) {

    const actionBtn = document.querySelector(`[data-file="actionBtn_${download.id}"]`)
    const delBtn = document.querySelector(`[data-file="delBtn_${download.id}"]`)

    if (download.isDownloading & !download.isDownloaded) {
        actionBtn.innerHTML = "Pause";
    } else {
        actionBtn.innerHTML = "Resume";
    }

    if (download.isDownloaded) {
        actionBtn.disabled = true;
        delBtn.disabled = true;
    } else {
        actionBtn.disabled = false;
        delBtn.disabled = false;
    }


    const downloadedBytes = document.querySelector(`[data-file="downloadedBytes_${download.id}"]`);

    if (downloadedBytes == null)
        downloadedBytes = 0;

    if (Number(downloadedBytes.dataset.size) != download.downloadedBytes) {
        downloadedBytes.textContent = `${getBytesToMb(download.downloadedBytes)} MB`;
    }


    const status = document.querySelector(`[data-file="status_${download.id}"]`)

    let html = "";

    if (!download.isMerget && download.isDownloaded) {

        html = `<div class="fw-semibold small text-primary" data-file="status_${download.id}">
                Pendiente unir <button type="button" class="btn btn-warning ms-2" data-file="merge_${download.id}" style="--bs-btn-padding-y: .15rem; --bs-btn-padding-x: .5rem; --bs-btn-font-size: .75rem;">UNIR</button>
            </div>`

        if (status.innerHTML !== html) {

            status.innerHTML = html;

            const mergeBtn = document.querySelector(`[data-file="merge_${download.id}"]`);

            if (mergeBtn !== null) {
                mergeBtn.addEventListener("click", () => {
                    forceMerge(download.id);
                })

                if (download.isMerget) {
                    mergeBtn.disabled = true;
                } else {
                    mergeBtn.disabled = false;
                }
            }
        }

        if (download.isMerging) {
            html = `<div class="fw-semibold small text-primary" data-file="status_${download.id}">
                            Uniendo ficheros
                        </div>`

            status.innerHTML = html;
        }

    } else {
        html =
            `<div class="fw-semibold small ${download.isDownloaded ? 'text-success' : 'text-primary'}" data-file="status_${download.id}">
                    ${download.isDownloaded ? 'Completed' : (download.isDownloading ? 'Downloading' : 'Paused')}
            </div>`

        status.innerHTML = html;
    }


    const progressNodes = document.querySelectorAll(`[data-file="progress_${download.id}"]`)

    const progress =
        download.length > 0
            ? Math.floor((download.downloadedBytes / download.length) * 100)
            : 0;


    progressNodes.forEach(node => {

        const progressText = `${progress}%`;

        node.textContent = progressText;

        if (node.dataset.type === "bar") {
            node.style.width = progressText;

            if (progress == 100)
                node.classList.add("bg-success");
        }
    });


    const partProgressNodes = document.querySelectorAll(`[data-file="partProgress_${download.id}"]`)

    let speedMax = 0;


    // Calculate % download progress
    download.parts.forEach(part => {

        const partDiv = document.querySelector(`[data-file="partProgress_${part.path}"]`)
        const spamProgress = document.querySelector(`[data-file="spamProgress_${part.path}"]`)

        const progressText = `${Math.floor((part.actualSize / part.length) * 100)}%`;

        partDiv.textContent = progressText;
        spamProgress.textContent = progressText;


        partDiv.style.width = progressText;
        if (progress == 100) {
            partDiv.classList.add("bg-success");
            spamProgress.classList.add("bg-success");
        }

        const speedDiv = document.querySelector(`[data-file="speed_${part.path}"]`);
        speedDiv.innerHTML = `${(part.actualSpeedFile / 1000 / 1000).toFixed(2)} Mbytes/s`;

        speedMax += part.actualSpeedFile;

    });



    const speedFile = document.querySelector(`[data-file="speedFile_${download.id}"]`)

    if (download.isDownloaded) {
        // On finish download, calculate averenge
        let speedAvg = 0;
        download.parts.forEach(part => {

            const speedDiv = document.querySelector(`[data-file="speed_${part.path}"]`);

            let partSpeedAvg = 0;

            part.registerSpeed.forEach(r => {

                partSpeedAvg += r;

            });

            speedDiv.innerHTML = `Avg: ${(partSpeedAvg / part.registerSpeed.length / 1000 / 1000).toFixed(2)} Mbytes/s`

            speedAvg += partSpeedAvg / part.registerSpeed.length;
        });

        speedFile.innerHTML = `Avg: ${(speedAvg / 1000 / 1000).toFixed(2)} Mbytes/s`

    } else {
        // Calculate total speed when download is running
        speedFile.innerHTML = `${(speedMax / 1000 / 1000).toFixed(2)} Mbytes/s`
    }
}

function getBytesToMb(downloadedBytes) {
    return ((downloadedBytes) / 1000 / 1000).toFixed(2);
}

function getBytesToGB(downloadedBytes) {
    return ((downloadedBytes) / 1000 / 1000 / 1000).toFixed(2);
}

stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
};

stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
};

function setConnected(connected) {
    console.log("CONNECTED: " + connected)
}

function connect() {
    stompClient.activate();
}

function createDownloadCard(download, index) {

    const progress =
        download.length > 0
            ? Math.floor((download.downloadedBytes / download.length) * 100)
            : 0;

    return `

    <div class="card shadow-sm border-0 mb-3 fade show" id="download_${download.id}">

        <div class="card-body py-3">

            <!-- HEADER -->
            <div class="d-flex justify-content-between align-items-start flex-wrap gap-2">

                <div>
                    <h6 class="card-title mb-0 text-break fw-semibold">
                        ${download.filePath.split('/').pop()}
                    </h6>

                    <small class="text-body-secondary">
                        ${download.filePath}
                    </small>
                </div>

                <!-- actions -->
                <div class="d-flex gap-2">

                    <button
                        class="btn btn-warning btn-sm py-1 px-3 pause-btn"
                        data-index="${index}"
                        data-file="actionBtn_${download.id}">

                        ${download.isDownloading ? 'Pause' : 'Resume'}

                    </button>

                    <button
                        class="btn btn-danger btn-sm py-1 px-3 cancel-btn"
                        data-index="${index}"
                        data-file="delBtn_${download.id}">Delete</button>
                </div>

            </div>

            <!-- INFO -->
            <div class="row mt-3 g-2">

                <div class="col-6 col-lg-2">
                    <div class="border rounded p-2 h-100">

                        <small class="text-body-secondary d-block">
                            Tamaño
                        </small>

                        <div class="fw-semibold small">
                            ${(download.length / 1000 / 1000).toFixed(2)} MB
                        </div>

                    </div>
                </div>

                <div class="col-6 col-lg-2">
                    <div class="border rounded p-2 h-100">

                        <small class="text-body-secondary d-block">
                            Descargado
                        </small>

                        <div class="fw-semibold small" data-file="downloadedBytes_${download.id}" data-size="${download.downloadedBytes}">
                            ${(download.downloadedBytes / 1000 / 1000).toFixed(2)} MB
                        </div>

                    </div>
                </div>

                <div class="col-6 col-lg-2">
                    <div class="border rounded p-2 h-100">

                        <small class="text-body-secondary d-block">
                            Fragmentos
                        </small>

                        <div class="fw-semibold small">
                            ${download.chunks}
                        </div>

                    </div>
                </div>

                <div class="col-6 col-lg-2">
                    <div class="border rounded p-2 h-100">

                        <small class="text-body-secondary d-block">
                            Velocidad (Mbytes/s)
                        </small>

                       <input type="number" class="form-control form-control-sm text-center mt-2"
                       id="speedLimit_${download.id}" name="speedLimit_${download.id}" required="" min="0.1" step="0.1" value="${download.speedLimit / 1000 / 1000}">

                    </div>
                </div>

                <div class="col-6 col-lg-4">
                    <div class="border rounded p-2 h-100">

                        <small class="text-body-secondary d-block">
                            Estado
                        </small>

                        <div class="fw-semibold small ${download.isDownloaded ? 'text-success' : 'text-primary'}" data-file="status_${download.id}">

                            ${download.isDownloaded ? 'Completed' : (download.isDownloading ? 'Downloading' : 'Paused')}

                        </div>

                    </div>
                </div>

            </div>

            <!-- TOTAL PROGRESS -->
            <div class="mt-3">

                <div class="d-flex justify-content-between mb-1">

                    <small class="fw-semibold">
                        Progreso total
                    </small>


                    <div class="fw-semibold small" data-file="speedFile_${download.id}">
                        SPEED: Calculando ..
                    </div>

                    <small data-file="progress_${download.id}" data-type="text">
                        ${progress}%
                        
                    </small>

                </div>

                <div class="progress"
                    style="height: 16px;">

                    <div class="progress-bar progress-bar-striped progress-bar-animated"
                        role="progressbar"
                        style="width: ${progress}%"
                        data-file="progress_${download.id}"
                        data-type="bar">

                        ${progress}%

                    </div>

                </div>

            </div>

            <!-- PARTS -->
            <div class="accordion accordion-flush mt-3"
                id="downloadAccordion_${index}">

                <div class="accordion-item">

                    <h2 class="accordion-header">

                        <button class="accordion-button collapsed py-2"
                            type="button"
                            data-bs-toggle="collapse"
                            data-bs-target="#collapseParts_${index}">

                            Fragmentos de descarga

                        </button>

                    </h2>

                    <div id="collapseParts_${index}"
                        class="accordion-collapse collapse"
                        data-bs-parent="#downloadAccordion_${index}">

                        <div class="accordion-body p-0">

                ${download.parts.map((part, partIndex) => {


        const partProgress = 0;

        return `

                            <div class="border-bottom p-2">

                                <div class="d-flex justify-content-between align-items-center mb-1">

                                    <div>

                                        <div class="fw-semibold small">
                                            PART_${partIndex}
                                        </div>

                                        <small class="text-body-secondary">
                                            ${(part.length / 1000 / 1000).toFixed(2)} MB
                                        </small>

                                    </div>

                                    <div class="fw-semibold small" data-file="speed_${part.path}">
                                        SPEED: ${part.speedFile}
                                    </div>

                                    <span class="badge text-bg-primary" data-file="spamProgress_${part.path}" data-type="text">
                                        ${partProgress}%
                                    </span>

                                </div>

                                <div class="progress"
                                    style="height: 12px;">

                                    <div class="progress-bar"
                                        role="progressbar"
                                        style="width: ${partProgress}%"
                                        data-file="partProgress_${part.path}"
                                        data-type="bar">


                                    </div>

                                </div>

                            </div>

                            `;
    }).join('')}

                        </div>

                    </div>

                </div>

            </div>

        </div>

    </div>

    `;
}