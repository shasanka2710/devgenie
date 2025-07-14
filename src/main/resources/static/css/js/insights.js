 console.log('Base script is loaded');

  // Dropdown filter logic for insights.html
  // Dropdown filter logic for insights.html
function filterIssues(selectedType) {
    const url = selectedType ? '/sonar/issues?filterType=' + encodeURIComponent(selectedType) : '/sonar/issues';
    window.location.href = url;
}

  document.addEventListener('DOMContentLoaded', function () {
      // Get the current page name from the data-page attribute
      console.log('Insights-specific script is running');

      // Attach event listener to the dropdown (if present)
      const issueTypeFilter = document.getElementById('issueTypeFilter');
      if (issueTypeFilter) {
          issueTypeFilter.addEventListener('change', function () {
              filterIssues(this.value);
          });
      }

      // Logic for "Apply Fix" buttons
      const fixButtons = document.querySelectorAll('button.apply-fix');
      fixButtons.forEach(button => {
          button.addEventListener('click', function (event) {
              event.preventDefault();
              const className = this.getAttribute('data-category');
              const description = this.getAttribute('data-description');
              const key = this.getAttribute('data-key');
              applyFix(this, className, description, key);
          });
      });

      // Logic for "Fix More" button
      const fixMoreButton = document.getElementById('fixMoreButton');
      const checkboxes = document.querySelectorAll('.issue-checkbox');

      checkboxes.forEach(checkbox => {
          checkbox.addEventListener('change', toggleFixMoreButton);
      });

      function toggleFixMoreButton() {
          const selectedCheckboxes = Array.from(checkboxes).filter(checkbox => checkbox.checked);
          fixMoreButton.style.display = selectedCheckboxes.length > 1 ? 'block' : 'none';
      }

      fixMoreButton.addEventListener('click', applyFixMore);

      function applyFixMore() {
          const selectedCheckboxes = Array.from(document.querySelectorAll('.issue-checkbox')).filter(checkbox => checkbox.checked);

          if (selectedCheckboxes.length === 0) {
              alert('No issues selected for fixing.');
              return;
          }

          const classDescriptions = selectedCheckboxes.map(checkbox => ({
              key: checkbox.getAttribute('data-key'),
              className: checkbox.getAttribute('data-category'),
              description: checkbox.getAttribute('data-description')
          }));

          // Get modal elements
          const modal = document.getElementById('progressModal');
          const progressBar = document.getElementById('progressBar');
          const progressText = document.getElementById('progressText');

          // Reset Modal
          progressBar.style.width = '0%';
          progressText.innerHTML = '<p>🔄 Initializing fix...</p>';

          // Ensure Close Button Exists
          let closeButton = document.getElementById('modalCloseButton');
          if (!closeButton) {
              closeButton = document.createElement('span');
              closeButton.innerHTML = '&times;';
              closeButton.id = 'modalCloseButton';
              closeButton.style.cssText = 'position: absolute; top: 10px; right: 15px; font-size: 28px; font-weight: bold; cursor: pointer;';
              closeButton.onclick = () => modal.style.display = 'none';
              modal.insertBefore(closeButton, modal.firstChild);
          }

          // Show modal before sending request
          modal.style.display = 'block';

          // **Step 1: Send Fix Request**
          fetch('/sonar/issue/apply-fix', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify(classDescriptions)
          })
          .then(response => response.json())
          .then(data => {
              if (!data.operationId) {
                  throw new Error('Operation ID missing in response.');
              }

              // **Start polling once operationId is received**
              trackFixProgress(data.operationId, progressBar, progressText);
          })
          .catch(error => {
              console.error('Error:', error);
              progressText.innerHTML += `<p style="color: red;">❌ Error: ${error.message}</p>`;
          });
      }//end of applyFixMore
      // **Step 2: Poll for Progress Updates**
      function trackFixProgress(operationId, progressBar, progressText) {
          let displayedEvents = new Set(); // Track displayed messages
          let progress = 0; // Track progress

          const interval = setInterval(() => {
              fetch(`/sonar/issue/fix-status/${operationId}`)
                  .then(response => response.json())
                  .then(data => {
                      if (!data.step || data.step.length === 0) return;

                  // Dynamically add new messages
                  data.step.forEach(event => {
                      if (!displayedEvents.has(event)) {
                          let newMessage = document.createElement('p');

                          // Check if the event contains HTML (like a hyperlink)
                          if (event.includes('<a href=')) {
                              newMessage.innerHTML = `${event}`; // Render as HTML
                          } else {
                              newMessage.textContent = `${event}`; // Render as plain text
                          }
                          progressText.appendChild(newMessage); // Append new message
                          displayedEvents.add(event);
                      }
                  });

                      // Update progress bar dynamically
                      progress += 15;
                      progressBar.style.width = Math.min(progress, 100) + '%';

                      // Stop polling if process is completed or failed
                      if (data.step.some(event => event.includes('✅ Completed'))) {
                          clearInterval(interval);
                          progressText.innerHTML += '<p style="color: green;">✅ Fix applied successfully!</p>';
                          progressBar.style.width = '100%';
                      }
                      if (data.step.some(event => event.includes('❌ Failed'))) {
                          clearInterval(interval);
                          progressText.innerHTML += '<p style="color: red;">❌ Fix failed.</p>';
                          progressBar.style.width = '100%';
                      }
                  })
                  .catch(error => {
                      console.error('Error fetching status:', error);
                      progressText.innerHTML += `<p style="color: orange;">⚠ Error checking progress.</p>`;
                      clearInterval(interval);
                  });
          }, 1000);
      }

        function applyFix(button, key, className, description) {
            const classDescriptions = [{ key, className, description }];
            applyFixMore();
        }
        function selectAllTop10(selectAllCheckbox) {
          const displayedRows = Array.from(rows).filter(row => row.style.display !== 'none');
          const top10Rows = displayedRows.slice(0, 10);
          top10Rows.forEach(row => {
              const checkbox = row.querySelector('.issue-checkbox');
              if (checkbox) {
                  checkbox.checked = selectAllCheckbox.checked;
              }
          });
          toggleFixMoreButton();
        }//end of selectAllTop10

        function toggleFixMoreButton() {
            const selectedCheckboxes = Array.from(checkboxes).filter(checkbox => checkbox.checked);
            fixMoreButton.style.display = selectedCheckboxes.length > 1 ? 'block' : 'none';
        }//end of toggleFixMoreButton

      function showModalMessage(message) {
          const modal = document.getElementById('progressModal');
          const modalContent = document.getElementById('progressModalContent');
          const progressBar = document.getElementById('progressBar');
          const progressText = document.getElementById('progressText');

          const closeButton = document.createElement('span');
          closeButton.innerHTML = '&times;';
          closeButton.style.cssText = 'position: absolute; top: 10px; right: 15px; font-size: 28px; font-weight: bold; cursor: pointer;';
          closeButton.onclick = () => {
              modal.style.display = 'none';
          };
          modalContent.insertBefore(closeButton, modalContent.firstChild);

          modal.style.display = 'block';
          progressBar.style.width = '0%';
          progressText.textContent = message;
      }
  });