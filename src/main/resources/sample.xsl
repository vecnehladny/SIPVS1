<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="/">
        <html>
            <head>
                <title>Person Information</title>
                <style>
                    *, ::after, ::before {
                    box-sizing: border-box;
                    }

                    body {
                    margin: 0;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol";
                    font-size: 1rem;
                    font-weight: 400;
                    line-height: 1.5;
                    color: #212529;
                    text-align: left;
                    background-color: #f8f9fa;
                    padding-top: 70px;
                    }

                    .container, .container-fluid {
                    width: 980px;
                    padding-right: 15px;
                    padding-left: 15px;
                    margin-right: auto;
                    margin-left: auto;
                    }

                    .container-fluid {
                    width: 100%;
                    }

                    .form-group {
                    margin-bottom: 1rem;
                    }

                    .form-control {
                    display: block;
                    width: 100%;
                    padding: 0.375rem 0.75rem;
                    font-size: 1rem;
                    line-height: 1.5;
                    color: #495057;
                    background-color: #fff;
                    background-clip: padding-box;
                    border: 1px solid #ced4da;
                    border-radius: 0.25rem;
                    transition: border-color .15s ease-in-out, box-shadow .15s ease-in-out;
                    }

                    .btn {
                    display: inline-block;
                    font-weight: 400;
                    text-align: center;
                    white-space: nowrap;
                    vertical-align: middle;
                    -webkit-user-select: none;
                    -moz-user-select: none;
                    -ms-user-select: none;
                    user-select: none;
                    border: 1px solid transparent;
                    padding: 0.375rem 0.75rem;
                    font-size: 1rem;
                    line-height: 1.5;
                    border-radius: 0.25rem;
                    transition: color .15s ease-in-out, background-color .15s ease-in-out, border-color .15s ease-in-out, box-shadow .15s ease-in-out;
                    }

                    .btn-primary {
                    color: #fff;
                    background-color: #007bff;
                    border-color: #007bff;
                    }

                    .btn-success {
                    color: #fff;
                    background-color: #28a745;
                    border-color: #28a745;
                    }

                    .btn-danger {
                    color: #fff;
                    background-color: #dc3545;
                    border-color: #dc3545;
                    }

                    .btn:not(:disabled):not(.disabled) {
                    cursor: pointer;
                    }

                    .btn-primary:hover {
                    color: #fff;
                    background-color: #0069d9;
                    border-color: #0062cc;
                    }

                    .btn-success:hover {
                    color: #fff;
                    background-color: #218838;
                    border-color: #1e7e34;
                    }

                    .btn-danger:hover {
                    color: #fff;
                    background-color: #c82333;
                    border-color: #bd2130;
                    }

                    .btn:focus, .btn:hover {
                    text-decoration: none;
                    }

                    .alert, .alert-toast {
                    --bs-alert-bg: transparent;
                    --bs-alert-padding-x: 1rem;
                    --bs-alert-padding-y: 1rem;
                    --bs-alert-margin-bottom: 1rem;
                    --bs-alert-color: inherit;
                    --bs-alert-border-color: transparent;
                    --bs-alert-border: 1px solid var(--bs-alert-border-color);
                    --bs-alert-border-radius: 0.375rem;
                    --bs-alert-link-color: inherit;
                    position: relative;
                    padding: var(--bs-alert-padding-y) var(--bs-alert-padding-x);
                    margin-bottom: var(--bs-alert-margin-bottom);
                    color: var(--bs-alert-color);
                    background-color: var(--bs-alert-bg);
                    border: var(--bs-alert-border);
                    border-radius: var(--bs-alert-border-radius);
                    margin-top: 0.5rem;
                    }

                    .alert-toast {
                    position: fixed;
                    bottom: 1rem;
                    right: 1rem;
                    z-index: 9999;
                    width: 25vw;
                    }

                    .alert-success {
                    --bs-alert-color: #0a3622;
                    --bs-alert-bg: #d1e7dd;
                    --bs-alert-border-color: #a3cfbb;
                    --bs-alert-link-color: #0a3622;
                    }

                    .alert-danger {
                    --bs-alert-color: #58151c;
                    --bs-alert-bg: #f8d7da;
                    --bs-alert-border-color: #f1aeb5;
                    --bs-alert-link-color: #58151c;
                    }

                    .alert-warning {
                    --bs-alert-color: #664d03;
                    --bs-alert-bg: #fff3cd;
                    --bs-alert-border-color: #ffe69c;
                    --bs-alert-link-color: #664d03;
                    }

                    .me-2 {
                    margin-right: 0.5rem !important;
                    }

                    .d-flex {
                    display: -webkit-box !important;
                    display: -ms-flexbox !important;
                    display: flex !important;
                    }

                    .justify-content-between {
                    -webkit-box-pack: justify !important;
                    -ms-flex-pack: justify !important;
                    justify-content: space-between !important;
                    }

                    .align-items-center {
                    -webkit-box-align: center !important;
                    -ms-flex-align: center !important;
                    align-items: center !important;
                    }

                    .card {
                    -bs-card-spacer-y: 1rem;
                    --bs-card-spacer-x: 1rem;
                    --bs-card-title-spacer-y: 0.5rem;
                    --bs-card-title-color: ;
                    --bs-card-subtitle-color: ;
                    --bs-card-border-width: 1px;
                    --bs-card-border-color: rgba(0, 0, 0, 0.175);
                    --bs-card-border-radius: 0.375rem;
                    --bs-card-box-shadow: ;
                    --bs-card-inner-border-radius: calc(0.375rem - 1px);
                    --bs-card-cap-padding-y: 0.5rem;
                    --bs-card-cap-padding-x: 1rem;
                    --bs-card-cap-bg: rgba(33, 37, 41, 0.03);
                    --bs-card-cap-color: ;
                    --bs-card-height: ;
                    --bs-card-color: ;
                    --bs-card-bg: #fff;
                    --bs-card-img-overlay-padding: 1rem;
                    --bs-card-group-margin: 0.75rem;
                    position: relative;
                    display: flex;
                    flex-direction: column;
                    min-width: 0;
                    height: var(--bs-card-height);
                    color: #212529;
                    word-wrap: break-word;
                    background-color: var(--bs-card-bg);
                    background-clip: border-box;
                    border: var(--bs-card-border-width) solid var(--bs-card-border-color);
                    border-radius: var(--bs-card-border-radius);
                    }

                    .card-body {
                    flex: 1 1 auto;
                    padding: 1rem 1rem;
                    color: var(--bs-card-color);
                    }
                    .w-100 {
                    width: 100% !important;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="card">
                        <div class="card-body">
                            <h1>Form</h1>
                            <xsl:apply-templates select="Person"/>
                        </div>
                    </div>
                </div>
            </body>
        </html>

    </xsl:template>

    <xsl:template match="Person">
        <form>
            <div class="form-group">
                <label for="personID">Person ID</label>
                <input class="form-control" type="text" id="personID" name="personID" value="{@personID}" disabled="disabled"/>
            </div>
            <div class="form-group">
                <label for="name">Name</label>
                <input class="form-control" type="text" id="name" name="name" value="{name}" disabled="disabled"/>
            </div>
            <div class="form-group">
                <label for="age">Age</label>
                <input class="form-control" type="number" id="age" name="age" value="{age}" disabled="disabled"/>
            </div>
            <div class="form-group">
                <label for="email">Email</label>
                <input class="form-control" type="email" id="email" name="email" value="{email}" disabled="disabled"/>
            </div>
            <div class="form-group">
                <label for="birthDate">Birth Date</label>
                <input class="form-control" type="date" id="birthDate" name="birthDate" value="{birthDate}" disabled="disabled"/>
            </div>
        </form>

        <div class="container-fluid">
        <div class="d-flex justify-content-between align-items-center">
            <h1>List of Children</h1>
        </div>
            <div id="childrenWrapper">
                <xsl:apply-templates select="children"/>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="children">
        <xsl:variable name="vPosition" select="position()"/>
                <div class="childRow">
                    <div class="d-flex justify-content-between align-items-center">
                        <h2>Child <span><xsl:value-of select="$vPosition"/></span></h2>
                    </div>
                    <div class="form-group">
                        <label>First name</label>
                        <input class="form-control" type="text" value="{firstName}" disabled="disabled"/>
                    </div>
                    <div class="form-group">
                        <label>Last Name</label>
                        <input class="form-control" type="text" value="{lastName}" disabled="disabled"/>
                    </div>
                </div>
    </xsl:template>


</xsl:stylesheet>
