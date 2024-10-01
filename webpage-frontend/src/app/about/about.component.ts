import {Component} from "@angular/core";

@Component({
  selector: "app-about",
  template: `
    <div class="container mx-auto my-2">
      <div class="p-2 border rounded">
        <h1 class="text-2xl font-bold mb-4">About TUManyBenchmarks</h1>
        <div class="mb-2">
          <h2 class="text-xl font-bold">Goal</h2>
          <p class="text-gray-700 text-justify">
            TUManyBenchmarks is a service dedicated to providing information about low-level performance metrics
            for different types of virtual machines.
            Our mission is to deliver accurate and reliable benchmark data to help users make informed
            decisions and achieve cost-efficiency.
          </p>
        </div>

        <div class="mb-2">
          <h2 class="text-xl font-bold">Motivation</h2>
          <p class="text-gray-700 text-justify">
            The lack of low-level performance metrics delivered by cloud providers and
            the difficulty of proper benchmarking leaded us to the conclusion that there is a need
            for a new benchmarking service.
            We believe such a service should characterize two attributes: extensibility and transparency.
            Extensibility means that every user should be able to upload their benchmark.
            This feature allow us for simultaneously collecting multiple metrics from many instances
            without unnecessary work and, as a result, allow using multiple metrics for comparison.
            The second characteristic is transparency.
            Transparency means everyone can check what and how benchmarks are executed to
            provide trustworthiness in the presented results.
            Moreover, the second aspect of transparency is achieving transparency of instances' performance
            within and between vendors by publishing their low-level performance metrics.
          </p>
        </div>

        <div>
          <h2 class="text-xl font-bold">Contribute</h2>
          <p class="text-gray-700">
            If you want to add a new benchmark create a Pull Request to our repository:
            <a href="https://github.com/Szczepaniak-M/TUManyBenchmarks-benchmarks">https://github.com/Szczepaniak-M/TUManyBenchmarks-benchmarks</a>
            <br/>
            To upload new benchmark, you have to provide:
          </p>
            <ul class="pr-4 text-gray-700 list-disc list-inside">
              <li>benchmark files - the program or script that will be executed by service</li>
              <li>configuration.yml - file specifying how to execute benchmark</li>
              <li>Ansible file - to prepare the benchmark environment and download dependencies</li>
              <li>Script formating benchmark output to JSON</li>
            </ul>
        </div>
      </div>
    </div>
  `
})
export class AboutComponent {
}
