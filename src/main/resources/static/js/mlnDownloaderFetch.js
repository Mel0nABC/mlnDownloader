/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */

document.addEventListener("DOMContentLoaded", () => {


    const urlInput = document.querySelector("#urlInput");
    const chucksInput = document.querySelector("#chucksInput");
    const fileNameInput = document.querySelector("#fileNameInput");

    const url = "https://releases.ubuntu.com/26.04/ubuntu-26.04-desktop-amd64.iso";
    const file = "ubuntu-26.04-desktop-amd64.iso";
    // const url = "https://es.mirrors.cicku.me/archlinux/iso/2026.05.01/archlinux-2026.05.01-x86_64.iso";
    // const file = "archlinux-2026.05.01-x86_64.iso";

    urlInput.value = url;
    chucksInput.value = 4;
    fileNameInput.value = file;

    urlInput.addEventListener("change", () => {
        try {
            const url = new URL(urlInput.value);
            const path = url.pathname;
            const fileName = path.substring(path.lastIndexOf("/") + 1);

            fileNameInput.value = fileName;
        } catch (e) {
            fileNameInput.value = "";
        }
    })

    const btn = document.querySelector("#btn");
    btn.addEventListener("click", () => {
        console.log("START DOWNLOAD")

        const urlInputText = urlInput.value.trim()
        const chucksInputText = chucksInput.value.trim()
        const fileNameInputText = fileNameInput.value.trim()


        if (urlInputText == "") {
            alert("Debes introducir una URL antes de continuar");
            return;
        }

        if (chucksInputText == "") {
            alert("Debes indicar cuantas partes simultáneas descargar");
            return;
        }

        if (fileNameInputText == "") {
            alert("Debes introducir un nombre de archivo");
            return;
        }


        download(urlInput.value, chucksInput.value, fileNameInput.value);
    })

    const cleanBtn = document.querySelector("#cleanBtn");
    cleanBtn.addEventListener("click", () => {
        console.log("CLEAN FINISH DOWNLOADS")
        cleanDownloads();
    })



})

async function download(urlInput, chucksInput, fileNameInput) {
    try {

        const response = await fetch("/api/downloads", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                uri: urlInput,
                chunks: chucksInput,
                fileName: fileNameInput
            })
        });

        const data = await response.json();

        if (!data.success) {
            alert(data.message)
        }

        console.log("Respuesta:", data.message);

    } catch (error) {
        console.error("Error:", error);
    }
}


export async function deleteDownloaded(id, downloads) {
    try {


        const response = await fetch("/api/downloads/" + id, {
            method: "DELETE"
        });

        const data = await response.json();

        if (!data.success) {
            alert(data.message)
        }

        console.log("Respuesta:", data.message);

    } catch (error) {
        console.error("Error:", error);
    }
}


export async function pauseOrResumeDownload(id) {
    try {
        const response = await fetch("/api/downloads/" + id, {
            method: "PUT"
        });

        const data = await response.json();

        if (!data.success) {
            alert(data.message)
        }

        console.log("Respuesta:", data.message);

    } catch (error) {
        console.error("Error:", error);
    }
}

async function cleanDownloads() {
    try {

        const response = await fetch("/api/downloads", {
            method: "DELETE"
        });

        const data = await response.json();

        if (!data.success) {
            alert(data.message)
        }

        console.log("Respuesta:", data.message);

    } catch (error) {
        console.error("Error:", error);
    }
}


export async function forceMerge(id) {
    try {

        const response = await fetch("/api/downloads/" + id, {
            method: "POST"
        });

        const data = await response.json();

        if (!data.success) {
            alert(data.message)
        }

        console.log("Respuesta:", data.message);

    } catch (error) {
        console.error("Error:", error);
    }
}
