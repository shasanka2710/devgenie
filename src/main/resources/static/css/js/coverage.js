let pathStack = [];

function loadChildren(parentPath) {
  pathStack.push(parentPath);
  $('#backLink').show();

  $.get('/coverage-dashboard/children', { parentPath: parentPath }, function (data) {
    let tbody = '';
    data.forEach(function (record) {
      let name = record.path.substring(record.path.lastIndexOf('/') + 1);
      let source;
      if (record.type === 'DIR') {
        source = `<span class='folder-icon'>📁</span><a href='#' class='source-link' data-path='${record.path}'>${name}</a>`;
      } else {
        source = name;
      }
      let overall = record.metricsMap && record.metricsMap.coverage != null ? record.metricsMap.coverage.toFixed(2) + '%' : '0%';
      let line = record.metricsMap && record.metricsMap.line_coverage != null ? record.metricsMap.line_coverage.toFixed(2) + '%' : '0%';
      let branch = record.metricsMap && record.metricsMap.branch_coverage != null ? record.metricsMap.branch_coverage.toFixed(2) + '%' : '0%';
      tbody += `<tr><td>${source}</td><td>${overall}</td><td>${line}</td><td>${branch}</td><td><button class='btn btn-primary btn-sm btn-improve'>🚀 Improve Coverage</button></td></tr>`;
    });
    $('#coverage-table-body').html(tbody);
    $('#currentPathLabel').text('Current Path: ' + parentPath);
  });
}

$(document).ready(function () {
  $(document).on('click', 'a.source-link', function (e) {
    e.preventDefault();
    const path = $(this).data('path');
    if (path) {
      loadChildren(path);
    }
  });

  $('#backLink').on('click', function () {
    pathStack.pop();
    const previousPath = pathStack.length > 0 ? pathStack[pathStack.length - 1] : '';

    if (previousPath === '') {
      $('#backLink').hide();
      window.location.href = '/coverage-dashboard';
    } else {
      loadChildren(previousPath);
    }
  });
});
