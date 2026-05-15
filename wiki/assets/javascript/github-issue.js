fetch('https://api.github.com/repos/mkdocs/mkdocs/issues')
    .then(response => response.json())
    .then(issues => {
        const container = document.getElementById('github-issues');
        issues.forEach(issue => {
            const issueElement = document.createElement('div');
            issueElement.innerHTML = `<a href="${issue.html_url}" target="_blank">#${issue.number}: ${issue.title}</a>`;
            container.appendChild(issueElement);
        });
    });