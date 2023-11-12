function selectFileAndSubmit(event) {
    var fileInput = document.getElementById('fileInput');
    var fileForm = document.getElementById('fileForm');
    fileForm.setAttribute('action', event.submitter.getAttribute('formaction'));

    event.preventDefault();
    fileInput.click();
    fileInput.addEventListener('change', function () {
        if (fileInput.files[0]) {
            document.getElementById('fileForm').submit();
        }
    });
}

function autoDeleteAlerts() {
    var alerts = document.querySelectorAll("#alerts .alert-toast");

    alerts.forEach(function (alerts) {
        setTimeout(function () {
            alerts.remove();
        }, 5000);
    });
}

function addAlert(code, message, type) {
    var alertBox = document.getElementById("alerts");
    alertBox.innerHTML += '<div class="alert-toast alert-' + type + '">\n' +
        '<p>' + code + ' ' + message + '</p>\n' +
        '</div>';
    autoDeleteAlerts();
}

function init() {
    autoDeleteAlerts();
    var wrapper = document.getElementById('childrenWrapper');
    var addChildButton = document.getElementById('addChild');

    addChildButton.addEventListener('click', function (event) {
        event.preventDefault();
        var htmlString = '<div class="childRow" id="child$IDXS$">\n' +
            '    <div class="d-flex justify-content-between align-items-center">\n' +
            '        <h2>Child $IDXS$</h2>\n' +
            '        <button class="btn btn-danger" id="removeChild$IDXS$">x</button>\n' +
            '    </div>\n' +
            '    <div class="form-group">\n' +
            '        <label for="childFirstName_$IDX$">First name</label>\n' +
            '        <input class="form-control" type="text" id="childFirstName_$IDX$" name="children[$IDX$].firstName" />\n' +
            '    </div>\n' +
            '    <div class="form-group">\n' +
            '        <label for="childLastName_$IDX$">Last Name</label>\n' +
            '        <input class="form-control" type="text" id="childLastName_$IDX$" name="children[$IDX$].lastName" />\n' +
            '    </div>\n' +
            '</div>';
        var tempElement = document.createElement('div');
        var childCount = wrapper.childElementCount;
        tempElement.innerHTML = htmlString.replaceAll('$IDX$', String(childCount))
                                          .replaceAll('$IDXS$', String(childCount + 1));
        wrapper.appendChild(tempElement.firstChild);

        var elements = document.querySelectorAll('[id^="removeChild"]');

        elements.forEach(function (element) {
            element.addEventListener('click', function (event) {
                event.preventDefault();
                var closestChildRow = element.closest('.childRow');
                if (closestChildRow) {
                    closestChildRow.remove();
                }
            });
        });
    });

    var elements = document.querySelectorAll('[id^="removeChild"]');

    elements.forEach(function (element) {
        element.addEventListener('click', function (event) {
            event.preventDefault();
            var closestChildRow = element.closest('.childRow');
            if (closestChildRow) {
                closestChildRow.remove();
            }
        });
    });
}

function Callback(onSuccess) {
    this.onSuccess = onSuccess;
    this.onError = function (e) {
        hideOverlay();
        if (ditec.utils.isDitecError(e)) {
            addAlert(e.code, e.message, "danger");
            console.log(e.detail);
        } else {
            addAlert("", "Error logged in console", "danger");
            console.log("Callback error | ", e);
        }
    };
}

function readFile(event) {
    var file = event.target.files[0];
    if (!file) return;
    var reader = new FileReader();
    reader.onload = function (e) {
        console.log("File Read | Success");
        var contents = e.target.result;
        signForm(contents);
    };
    reader.onerror = function (error) {
        console.log('File Read | Error ', error);
    };

    return reader.readAsText(file)
}

function downloadSignature(ret) {
    const blob = new Blob([ret], { type: 'application/xml' });
    const link = document.createElement('a');
    link.href = window.URL.createObjectURL(blob);
    link.download = "xades_signature.xml";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

function showOverlay() {
    var overlay = document.getElementById('overlay');
    overlay.style.display = 'block';
}

function hideOverlay() {
    var overlay = document.getElementById('overlay');
    overlay.style.display = 'none';
}

function signForm(xmlFile) {
    var xsdFile = document.getElementById('sign_xsdFile').value;
    var xslFile = document.getElementById('sign_xslFile').value;
    var pdfFile = document.getElementById('sign_pdfFile').value;
    ditec.dSigXadesJs.deploy(null, new Callback(function () {
        ditec.dSigXadesJs.initialize(new Callback(function () {
            showOverlay();
            ditec.dSigXadesJs.addXmlObject2("form_id", "Form", xmlFile, xsdFile, "http://www.example.com", "http://www.w3.org/2001/XMLSchema", xslFile, "http://www.w3.org/1999/XSL/Transform", "HTML", new Callback(function () {
                ditec.dSigXadesJs.addPdfObject("pdf_id", "PDF File", pdfFile, "", "http://example.com/objectFormatIdentifier", 2, false, new Callback(function () {
                    ditec.dSigXadesJs.sign20("signatureId", "http://www.w3.org/2001/04/xmlenc#sha256", "urn:oid:1.3.158.36061701.1.2.3", "dataEnvelopeId", "http://dataEnvelopeURI", "dataEnvelopeDescr", new Callback(function () {
                        ditec.dSigXadesJs.getSignedXmlWithEnvelope(new Callback(function (ret) {
                            hideOverlay();
                            downloadSignature(ret);
                            addAlert("", "Document was signed successfully", "success");
                            console.log(ret);
                        }));
                    }));
                }));
            }));
        }));
    }));
}