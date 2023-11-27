document.addEventListener('DOMContentLoaded', function () {
    var dropArea = document.getElementById('drop-area');

    ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
        dropArea.addEventListener(eventName, preventDefaults, false)
    });

    function preventDefaults(e) {
        e.preventDefault();
        e.stopPropagation();
    }

    ['dragenter', 'dragover'].forEach(eventName => {
        dropArea.addEventListener(eventName, highlight, false)
    });

    ['dragleave', 'drop'].forEach(eventName => {
        dropArea.addEventListener(eventName, unhighlight, false)
    });

    dropArea.addEventListener('drop', handleDrop, false);

    function highlight() {
        dropArea.classList.add('highlight');
    }

    function unhighlight() {
        dropArea.classList.remove('highlight');
    }

    function handleDrop(e) {
        var dt = e.dataTransfer;
        var files = dt.files;

        handleFiles(files);
    }

    function visualizeResult(success) {
        addAlert("", "Files uploaded successfully", "success")
        let uploadResultBox = document.getElementById('upload-result');
        uploadResultBox.innerHTML = '';
        success.forEach((item) => {
            console.log(item);
            var cardDiv = document.createElement("div");
            cardDiv.className = "alert card alert-" + item.resultClass;

            var title = document.createElement("h5");
            title.className = "alert-heading";
            title.textContent = item.filename;

            cardDiv.appendChild(title);

            if(item.exception) {
                var exception = document.createElement("p");
                exception.innerHTML = item.exception.message;
                // if(item.exception.cause) {
                //     var cause = document.createElement("p");
                //     cause.innerHTML = item.exception.cause.message;
                //     exception.appendChild(cause);
                // }
                cardDiv.appendChild(exception);
            }

            uploadResultBox.appendChild(cardDiv);
        });
    }

    function visualizeErrorResult(error) {
        addAlert("", "Error uploading files", "danger")
    }

    function handleFiles(files) {
        var formData = new FormData();

        for (var i = 0; i < files.length; i++) {
            formData.append('files', files[i]);
        }

        fetch('/validator/upload', {
            method: 'POST',
            body: formData
        })
            .then(response => response.json())
            .then(success => visualizeResult(success))
            .catch(error => visualizeErrorResult(error));
    }
});