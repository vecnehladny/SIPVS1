<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="/">
        <html>
            <head>
                <title>Person Information</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol";
                        font-size: 1rem;
                        font-weight: 400;
                        line-height: 1.5;
                        color: #212529;
                        text-align: left;
                        background-color: #f8f9fa;
                    }
                    .container {
                        max-width: 800px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    h1 {
                        font-size: 24px;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        border: 1px solid #ccc;
                        margin-bottom: 20px;
                    }
                    th, td {
                        border: 1px solid #ccc;
                        padding: 8px;
                        text-align: left;
                    }
                    th {
                        background-color: #f2f2f2;
                        width: 20%;
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
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="card">
                        <div class="card-body">
                            <h1>Person Information</h1>
                            <xsl:apply-templates select="Person"/>
                        </div>
                    </div>
                </div>
            </body>
        </html>

    </xsl:template>

    <xsl:template match="Person">
        <table border="1">
            <tr>
                <th>Person ID</th>
                <td>
                    <xsl:value-of select="@personID"/>
                </td>
            </tr>
            <tr>
                <th>Name</th>
                <td>
                    <xsl:value-of select="name"/>
                </td>
            </tr>
            <tr>
                <th>Age</th>
                <td>
                    <xsl:value-of select="age"/>
                </td>
            </tr>
            <tr>
                <th>Email</th>
                <td>
                    <xsl:value-of select="email"/>
                </td>
            </tr>
            <tr>
                <th>Birth Date</th>
                <td>
                    <xsl:value-of select="birthDate"/>
                </td>
            </tr>
        </table>

        <xsl:if test="children">
            <h1>Children</h1>
            <xsl:apply-templates select="children"/>
        </xsl:if>
    </xsl:template>

    <xsl:template match="children">
        <table>
            <tr>
                <th>First Name</th>
                <td>
                    <xsl:value-of select="firstName"/>
                </td>
            </tr>
            <tr>
                <th>Last Name</th>
                <td>
                    <xsl:value-of select="lastName"/>
                </td>
            </tr>
        </table>
    </xsl:template>


</xsl:stylesheet>
